package com.example.atlethiq.data

import com.example.atlethiq.data.db.DailySnapshotDao
import com.example.atlethiq.data.db.MetricSampleDao
import com.example.atlethiq.data.models.DailySnapshot
import com.example.atlethiq.data.models.MetricSample
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

interface DataRepository {
    val data: Flow<List<String>>
    fun getDailySnapshot(day: String): Flow<DailySnapshot?>
    suspend fun getDailySnapshotSync(day: String): DailySnapshot?
    suspend fun getHrvSamplesForSparkline(endDateStr: String): List<Double>
    suspend fun loadSeededDataFromSupabase(userId: String)
    suspend fun getAllDailySnapshots(): List<DailySnapshot>
    /** Returns the most recent date string (yyyy-MM-dd) that has call != 'no_data', or null if none. */
    suspend fun getLatestDayWithData(): String?
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
            val dateStr = java.time.Instant.ofEpochMilli(sample.startTime).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
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
}

