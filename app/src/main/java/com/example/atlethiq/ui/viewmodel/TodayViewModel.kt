package com.example.atlethiq.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atlethiq.data.AppStateStore
import com.example.atlethiq.data.DataRepository
import com.example.atlethiq.data.LogHistoryRow
import com.example.atlethiq.data.SourceSummary
import com.example.atlethiq.data.TrendsChartData
import com.example.atlethiq.data.models.DailySnapshot
import com.example.atlethiq.work.MorningCallWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val supabaseClient: SupabaseClient,
    private val appStateStore: AppStateStore,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    /** Latest day index that has a real (non-no_data) snapshot; used to clamp the frontier. */
    private var latestRealDay = AppStateStore.DAY_MAX

    /** One-shot signal to force the UI to the Today tab (e.g. after a notification tap). */
    private val _navToTodayForDay = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val navToTodayForDay: SharedFlow<Int> = _navToTodayForDay.asSharedFlow()

    private val _dayIndex = MutableStateFlow(45)
    val dayIndex: StateFlow<Int> = _dayIndex.asStateFlow()

    private val _dateString = MutableStateFlow("2026-07-10")
    val dateString: StateFlow<String> = _dateString.asStateFlow()

    private val _currentSnapshot = MutableStateFlow<DailySnapshot?>(null)
    val currentSnapshot: StateFlow<DailySnapshot?> = _currentSnapshot.asStateFlow()

    private val _sparklineHrv = MutableStateFlow<List<Double>>(emptyList())
    val sparklineHrv: StateFlow<List<Double>> = _sparklineHrv.asStateFlow()

    private val _debugMode = MutableStateFlow(false)
    val debugMode: StateFlow<Boolean> = _debugMode.asStateFlow()

    private val _showDecodeSheet = MutableStateFlow(false)
    val showDecodeSheet: StateFlow<Boolean> = _showDecodeSheet.asStateFlow()

    private val _trendsData = MutableStateFlow<TrendsChartData?>(null)
    val trendsData: StateFlow<TrendsChartData?> = _trendsData.asStateFlow()

    private val _sourceSummaries = MutableStateFlow<List<SourceSummary>>(emptyList())
    val sourceSummaries: StateFlow<List<SourceSummary>> = _sourceSummaries.asStateFlow()

    private val _logHistory = MutableStateFlow<List<LogHistoryRow>>(emptyList())
    val logHistory: StateFlow<List<LogHistoryRow>> = _logHistory.asStateFlow()

    private val _logSubmitting = MutableStateFlow(false)
    val logSubmitting: StateFlow<Boolean> = _logSubmitting.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Ensure local db has mock user's daily snapshots and samples
                val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    dataRepository.loadSeededDataFromSupabase(userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _trendsData.value = dataRepository.getTrendsChartData()
            refreshLogHistory()

            // Resolve the latest day that has a real computed snapshot (MAX(day) WHERE call !=
            // 'no_data'), so we never open on a no_data placeholder for a partial day.
            val latestDay = dataRepository.getLatestDayWithData()
            latestRealDay = latestDay?.let { AppStateStore.dayForDate(it).coerceIn(1, 45) } ?: 45

            // The frontier is the app's current latest-with-data day (M4). On first launch it starts
            // at the latest real day, and lastNotified is pinned to it so no notification fires blind
            // for pre-existing data — only genuine advances (a "new morning") post a Call.
            if (appStateStore.frontierDay < 1) {
                appStateStore.frontierDay = latestRealDay
                appStateStore.lastNotifiedDay = latestRealDay
            }
            val frontier = appStateStore.frontierDay.coerceIn(1, 45)
            _dayIndex.value = frontier
            loadDay(frontier)

            // Re-run the morning check on every open. If the frontier hasn't advanced past
            // lastNotified, the worker no-ops — this is what makes reopening a notified day silent.
            MorningCallWorker.enqueue(appContext)
        }
    }

    /**
     * Debug: "Simulate morning data arrival." Advances the frontier one day within the seed range
     * and enqueues the WorkManager job, which detects the new day and fires the notification
     * naturally. This is the real daily-loop mechanism — it does not post a notification directly.
     */
    fun advanceSimulatedDay() {
        val next = appStateStore.frontierDay + 1
        if (next > AppStateStore.DAY_MAX) return
        appStateStore.frontierDay = next
        selectDay(next)
        MorningCallWorker.enqueue(appContext)
    }

    /**
     * Debug: repositions the frontier to (selected day − 1) and clears the notified pointer to just
     * below it, so the next "Simulate morning data arrival" advances onto the selected day and fires
     * its Call. Repositioning only — it never posts a notification itself.
     */
    fun armSimulationAtSelectedDay() {
        val target = (_dayIndex.value - 1).coerceIn(1, AppStateStore.DAY_MAX)
        appStateStore.frontierDay = target
        appStateStore.lastNotifiedDay = target
        selectDay(target)
    }

    /**
     * Consumes a pending notification deep-link, if any: scopes Today to the tapped day and signals
     * the UI to switch to the Today tab. Called on each resume so both cold and warm starts work.
     */
    fun handlePendingNotificationDeepLink() {
        val day = appStateStore.pendingNotifDay
        if (day in 1..45) {
            appStateStore.pendingNotifDay = 0
            selectDay(day)
            _navToTodayForDay.tryEmit(day)
        }
    }

    fun selectDay(index: Int) {
        if (index in 1..45) {
            _dayIndex.value = index
            loadDay(index)
        }
    }

    /** Resolves a "yyyy-MM-dd" date to its 1..45 day index and selects it. Used by the Log history list. */
    fun selectDayByDate(dateStr: String) {
        val baseDate = LocalDate.parse("2026-07-10")
        val diffDays = ChronoUnit.DAYS.between(LocalDate.parse(dateStr), baseDate)
        val idx = (45 - diffDays).toInt().coerceIn(1, 45)
        selectDay(idx)
    }

    fun toggleDebugMode() {
        _debugMode.value = !_debugMode.value
    }

    fun attributionRows() = dataRepository.getAttributionRows()

    fun setShowDecodeSheet(show: Boolean) {
        _showDecodeSheet.value = show
    }

    fun logManualEntry(rpe: Int?, feel: String?, note: String?) {
        viewModelScope.launch {
            _logSubmitting.value = true
            try {
                val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    dataRepository.logManualEntry(userId, _dateString.value, rpe, feel, note)
                    refreshLogHistory()
                    refreshSourceSummaries(_dateString.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _logSubmitting.value = false
        }
    }

    private suspend fun refreshLogHistory() {
        _logHistory.value = dataRepository.getLogHistory()
    }

    private suspend fun refreshSourceSummaries(dateStr: String) {
        _sourceSummaries.value = dataRepository.getSourceSummaries(dateStr)
    }

    private fun loadDay(index: Int) {
        viewModelScope.launch {
            // Day 45 is 2026-07-10. Day 1 is 2026-07-10 minus 44 days (2026-05-27)
            val baseDate = LocalDate.parse("2026-07-10")
            val targetDate = baseDate.minusDays((45 - index).toLong())
            val dateStr = targetDate.toString()
            _dateString.value = dateStr

            val snapshot = dataRepository.getDailySnapshotSync(dateStr)
            _currentSnapshot.value = snapshot

            val sparkline = dataRepository.getHrvSamplesForSparkline(dateStr)
            _sparklineHrv.value = sparkline

            refreshSourceSummaries(dateStr)
        }
    }
}
