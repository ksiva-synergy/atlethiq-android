package com.example.atlethiq.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.atlethiq.data.models.MetricSample
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetricSamples(samples: List<MetricSample>): List<Long>

    @Query("SELECT * FROM metric_samples ORDER BY startTime DESC")
    fun getAllMetricSamplesFlow(): Flow<List<MetricSample>>

    @Query("SELECT * FROM metric_samples WHERE startTime >= :since ORDER BY startTime DESC")
    suspend fun getMetricSamplesSince(since: Long): List<MetricSample>
}
