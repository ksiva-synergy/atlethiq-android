package com.example.atlethiq.data

import com.example.atlethiq.data.db.DailySnapshotDao
import com.example.atlethiq.data.db.MetricSampleDao
import com.example.atlethiq.data.models.DailySnapshot
import com.example.atlethiq.data.models.MetricSample
import com.example.atlethiq.domain.dedup.PlaceholderDedupConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Network-only DTO mirroring the `metric_samples` Supabase columns exactly (see supabase_schema.sql). */
@Serializable
data class MetricSampleUpsert(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("metric_type") val metricType: String,
    val value: Double?,
    val unit: String?,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String?,
    @SerialName("zone_offset") val zoneOffset: String?,
    @SerialName("source_app") val sourceApp: String,
    @SerialName("source_device") val sourceDevice: String? = null,
    @SerialName("recording_method") val recordingMethod: String,
    @SerialName("client_sample_id") val clientSampleId: String,
    val payload: JsonElement? = null,
)

data class TrendsChartData(
    val dates: List<String>,
    val hrv7d: List<Double>,
    val hrv30dBaseline: List<Double>,
    val rhr7d: List<Double>,
    val rhr30dBaseline: List<Double>,
    val sleepHours: List<Double>,
    val sleepNeedHours: Double,
    val load7d: List<Double>,
    val load28dWeeklyAvg: List<Double>,
)

data class SourceSummary(
    val sourceApp: String,
    val lastDataAtMillis: Long?,
    val flowingToday: Boolean,
    val neverConnected: Boolean,
    val dataTypeChips: List<String>,
)

data class AttributionRow(
    val metricLabel: String,
    val winner: String,
    val dedupedNote: String?,
)

data class LogHistoryRow(
    val date: String,
    val call: String,
    val confidence: String,
    val sessionSummary: String?,
    val feelLabel: String?,
    val rpe: Int?,
)

interface DataRepository {
    val data: Flow<List<String>>
    fun getDailySnapshot(day: String): Flow<DailySnapshot?>
    suspend fun getDailySnapshotSync(day: String): DailySnapshot?
    suspend fun getHrvSamplesForSparkline(endDateStr: String): List<Double>
    suspend fun loadSeededDataFromSupabase(userId: String)
    suspend fun getAllDailySnapshots(): List<DailySnapshot>
    /** Returns the most recent date string (yyyy-MM-dd) that has call != 'no_data', or null if none. */
    suspend fun getLatestDayWithData(): String?
    suspend fun getTrendsChartData(): TrendsChartData
    suspend fun getSourceSummaries(selectedDateStr: String): List<SourceSummary>
    fun getAttributionRows(): List<AttributionRow>
    suspend fun getLogHistory(): List<LogHistoryRow>
    suspend fun logManualEntry(userId: String, date: String, rpe: Int?, feel: String?, note: String?)
}

@Singleton
class DefaultDataRepository @Inject constructor(
    private val dailySnapshotDao: DailySnapshotDao,
    private val metricSampleDao: MetricSampleDao,
    private val supabaseClient: SupabaseClient
) : DataRepository {
    override val data: Flow<List<String>> = flow { emit(listOf("Android")) }

    override fun getDailySnapshot(day: String): Flow<DailySnapshot?> =
        dailySnapshotDao.getDailySnapshotFlow(day)

    override suspend fun getDailySnapshotSync(day: String): DailySnapshot? =
        dailySnapshotDao.getDailySnapshot(day)

    override suspend fun getAllDailySnapshots(): List<DailySnapshot> =
        dailySnapshotDao.getAllDailySnapshots()

    override suspend fun getLatestDayWithData(): String? =
        dailySnapshotDao.getLatestDayWithData()

    override suspend fun getHrvSamplesForSparkline(endDateStr: String): List<Double> {
        val localDate = LocalDate.parse(endDateStr)
        val endEpoch = localDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli()
        val startEpoch = localDate.minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

        // Fetch HRV samples
        val samples = metricSampleDao.getMetricSamplesForSparkline("hrv_rmssd", startEpoch, endEpoch)
        // Group by day to find the priority source sample (mock_whoop > mock_oneplus)
        val dailyMap = mutableMapOf<String, Double>()
        for (sample in samples) {
            val dateStr = dateOf(sample.startTime)
            val existingVal = dailyMap[dateStr]
            if (existingVal == null || sample.sourceApp == "mock_whoop") {
                dailyMap[dateStr] = sample.value ?: 0.0
            }
        }

        // Fill sparkline list chronologically for 7 days
        val result = mutableListOf<Double>()
        for (i in 0..6) {
            val dStr = localDate.minusDays(6 - i.toLong()).toString()
            result.add(dailyMap[dStr] ?: 0.0)
        }
        return result
    }

    override suspend fun loadSeededDataFromSupabase(userId: String) {
        // Fetch all metric samples and snapshots seeded on Supabase and populate local DB
        try {
            val snapshots = supabaseClient.postgrest.from("daily_snapshots").select().decodeList<DailySnapshot>()
            for (snapshot in snapshots) {
                val dbSnapshot = snapshot.copy(
                    decodeJson = snapshot.decode.toString(),
                    debugJson = snapshot.debug.toString()
                )
                dailySnapshotDao.insertDailySnapshot(dbSnapshot)
            }

            val samples = supabaseClient.postgrest.from("metric_samples").select().decodeList<MetricSample>()
            val mappedSamples = samples.map { sample ->
                val startMilli = try {
                    java.time.OffsetDateTime.parse(sample.startTimeString).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    0L
                }
                val endMilli = try {
                    sample.endTimeString?.let { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }
                } catch (e: Exception) {
                    null
                }
                sample.copy(
                    startTime = startMilli,
                    endTime = endMilli,
                    payloadJson = sample.payload?.toString() ?: ""
                )
            }
            metricSampleDao.insertMetricSamples(mappedSamples)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getTrendsChartData(): TrendsChartData {
        val snapshots = dailySnapshotDao.getAllDailySnapshots() // ASC by day
        val sleepNeed = 7.5

        val dates = mutableListOf<String>()
        val hrv7d = mutableListOf<Double>()
        val hrv30d = mutableListOf<Double>()
        val rhr7d = mutableListOf<Double>()
        val rhr30d = mutableListOf<Double>()
        val sleepHours = mutableListOf<Double>()
        val load7d = mutableListOf<Double>()
        val load28dAvg = mutableListOf<Double>()

        for (snap in snapshots) {
            dates.add(snap.day)
            val debug = try {
                Json.parseToJsonElement(snap.debugJson).jsonObject
            } catch (e: Exception) {
                null
            }
            fun field(name: String): Double = debug?.get(name)?.jsonPrimitive?.doubleOrNull ?: 0.0

            hrv7d.add(field("hrv_mean_7d"))
            hrv30d.add(field("hrv_mean_30d"))
            rhr7d.add(field("rhr_mean_7d"))
            rhr30d.add(field("rhr_mean_30d"))
            sleepHours.add((field("sleep_score") / 100.0) * sleepNeed)
            load7d.add(field("load_7d_sum"))
            load28dAvg.add(field("load_28d_sum") / 4.0)
        }

        return TrendsChartData(
            dates = dates,
            hrv7d = hrv7d,
            hrv30dBaseline = hrv30d,
            rhr7d = rhr7d,
            rhr30dBaseline = rhr30d,
            sleepHours = sleepHours,
            sleepNeedHours = sleepNeed,
            load7d = load7d,
            load28dWeeklyAvg = load28dAvg
        )
    }

    override suspend fun getSourceSummaries(selectedDateStr: String): List<SourceSummary> {
        val selectedDate = LocalDate.parse(selectedDateStr)
        val selectedEndEpoch = selectedDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli()
        val allSamples = metricSampleDao.getAllMetricSamplesSync()
        val bySource = allSamples.groupBy { it.sourceApp }

        val knownSources = listOf("mock_whoop", "mock_oneplus", "manual")
        return knownSources.map { app ->
            val samplesUpToDate = bySource[app].orEmpty().filter { it.startTime <= selectedEndEpoch }
            val last = samplesUpToDate.maxByOrNull { it.startTime }
            val flowingToday = samplesUpToDate.any { dateOf(it.startTime) == selectedDateStr }
            val neverConnected = samplesUpToDate.isEmpty()
            val dataTypes = samplesUpToDate.map { it.metricType }.distinct()
                .map { PlaceholderDedupConfig.metricDisplayNames[it] ?: it }
                .sorted()
            SourceSummary(
                sourceApp = app,
                lastDataAtMillis = last?.startTime,
                flowingToday = flowingToday,
                neverConnected = neverConnected,
                dataTypeChips = dataTypes
            )
        }
    }

    override fun getAttributionRows(): List<AttributionRow> {
        return PlaceholderDedupConfig.priorityByMetricType.map { (metricType, sources) ->
            val label = PlaceholderDedupConfig.metricDisplayNames[metricType] ?: metricType
            val winner = sources.first()
            val dedupedNote = if (sources.size > 1) {
                "(${sources.drop(1).joinToString(", ")} deduped)"
            } else {
                null
            }
            AttributionRow(metricLabel = label, winner = winner, dedupedNote = dedupedNote)
        }
    }

    override suspend fun getLogHistory(): List<LogHistoryRow> {
        val snapshots = dailySnapshotDao.getAllDailySnapshots() // ASC
        val allSamples = metricSampleDao.getAllMetricSamplesSync()

        val exerciseByDate = allSamples.filter { it.metricType == "exercise_session" }.groupBy { dateOf(it.startTime) }
        val rpeByDate = allSamples.filter { it.metricType == "rpe" }.groupBy { dateOf(it.startTime) }
        val feelByDate = allSamples.filter { it.metricType == "subjective_feel" }.groupBy { dateOf(it.startTime) }

        return snapshots.reversed().map { snap ->
            val date = snap.day
            val exercises = exerciseByDate[date]
            val sessionSummary = exercises?.takeIf { it.isNotEmpty() }?.let { list ->
                val totalMin = list.sumOf { it.value ?: 0.0 }
                val totalLoad = list.sumOf { ex ->
                    val intensity = try {
                        Json.parseToJsonElement(ex.payloadJson).jsonObject["intensity_proxy"]?.jsonPrimitive?.doubleOrNull ?: 1.0
                    } catch (e: Exception) {
                        1.0
                    }
                    (ex.value ?: 0.0) * intensity
                }
                "Session · ${totalMin.toInt()} min · load ${totalLoad.toInt()}"
            }
            val rpeVal = rpeByDate[date]?.maxByOrNull { it.startTime }?.value?.toInt()
            val feelRaw = feelByDate[date]?.maxByOrNull { it.startTime }?.let { sample ->
                try {
                    Json.parseToJsonElement(sample.payloadJson).jsonObject["feel"]?.jsonPrimitive?.content
                } catch (e: Exception) {
                    null
                }
            }
            LogHistoryRow(
                date = date,
                call = snap.call,
                confidence = snap.confidence,
                sessionSummary = sessionSummary,
                feelLabel = feelDisplayLabel(feelRaw),
                rpe = rpeVal
            )
        }
    }

    override suspend fun logManualEntry(userId: String, date: String, rpe: Int?, feel: String?, note: String?) {
        val nowMillis = System.currentTimeMillis()
        val localEntries = mutableListOf<MetricSample>()
        val remoteEntries = mutableListOf<MetricSampleUpsert>()

        if (rpe != null) {
            val id = UUID.randomUUID().toString()
            val clientId = "manual_${date}_${nowMillis}_rpe"
            val timestampIso = "${date}T19:30:00+00:00"
            val epochMillis = OffsetDateTime.parse(timestampIso).toInstant().toEpochMilli()
            localEntries.add(
                MetricSample(
                    id = id,
                    metricType = "rpe",
                    value = rpe.toDouble(),
                    unit = "scale_1_10",
                    startTimeString = timestampIso,
                    endTimeString = timestampIso,
                    zoneOffset = "+00:00",
                    sourceApp = "manual",
                    sourceDevice = null,
                    recordingMethod = "manual",
                    clientSampleId = clientId,
                    startTime = epochMillis,
                    endTime = epochMillis
                )
            )
            remoteEntries.add(
                MetricSampleUpsert(
                    id = id,
                    userId = userId,
                    metricType = "rpe",
                    value = rpe.toDouble(),
                    unit = "scale_1_10",
                    startTime = timestampIso,
                    endTime = timestampIso,
                    zoneOffset = "+00:00",
                    sourceApp = "manual",
                    recordingMethod = "manual",
                    clientSampleId = clientId
                )
            )
        }

        if (feel != null || note != null) {
            val id = UUID.randomUUID().toString()
            val clientId = "manual_${date}_${nowMillis}_feel"
            val timestampIso = "${date}T19:35:00+00:00"
            val epochMillis = OffsetDateTime.parse(timestampIso).toInstant().toEpochMilli()
            val payloadObj = buildJsonObject {
                if (feel != null) put("feel", feel)
                if (note != null) put("note", note)
            }
            localEntries.add(
                MetricSample(
                    id = id,
                    metricType = "subjective_feel",
                    value = null,
                    unit = "category",
                    startTimeString = timestampIso,
                    endTimeString = timestampIso,
                    zoneOffset = "+00:00",
                    sourceApp = "manual",
                    sourceDevice = null,
                    recordingMethod = "manual",
                    clientSampleId = clientId,
                    startTime = epochMillis,
                    endTime = epochMillis,
                    payloadJson = payloadObj.toString()
                )
            )
            remoteEntries.add(
                MetricSampleUpsert(
                    id = id,
                    userId = userId,
                    metricType = "subjective_feel",
                    value = null,
                    unit = "category",
                    startTime = timestampIso,
                    endTime = timestampIso,
                    zoneOffset = "+00:00",
                    sourceApp = "manual",
                    recordingMethod = "manual",
                    clientSampleId = clientId,
                    payload = payloadObj
                )
            )
        }

        if (localEntries.isEmpty()) return

        metricSampleDao.insertMetricSamples(localEntries)
        supabaseClient.postgrest.from("metric_samples").insert(remoteEntries)
    }

    private fun dateOf(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate().toString()

    private fun feelDisplayLabel(raw: String?): String? = when (raw) {
        "felt_fresh" -> "Fresh"
        "normal" -> "Normal"
        "felt_flat" -> "Flat"
        "felt_wrecked" -> "Wrecked"
        else -> null
    }
}
