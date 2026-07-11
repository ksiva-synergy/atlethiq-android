package com.example.atlethiq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atlethiq.data.DataRepository
import com.example.atlethiq.data.models.DailySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

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

            // Use MAX(day) WHERE call != 'no_data' so we open on the most recent real snapshot,
            // not a no_data placeholder produced for the current partial day.
            val latestDay = dataRepository.getLatestDayWithData()
            if (latestDay != null) {
                val diffDays = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.parse("2026-05-27"),
                    LocalDate.parse(latestDay)
                )
                val dayIdx = (diffDays + 1).toInt().coerceIn(1, 45)
                _dayIndex.value = dayIdx
                loadDay(dayIdx)
            } else {
                loadDay(45)
            }
        }
    }

    fun selectDay(index: Int) {
        if (index in 1..45) {
            _dayIndex.value = index
            loadDay(index)
        }
    }

    fun toggleDebugMode() {
        _debugMode.value = !_debugMode.value
    }

    fun setShowDecodeSheet(show: Boolean) {
        _showDecodeSheet.value = show
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
        }
    }
}
