package com.example.atlethiq.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "metric_samples",
    indices = [
        Index(value = ["metricType", "clientSampleId"], unique = true)
    ]
)
@Serializable
data class MetricSample(
    @PrimaryKey val id: String, // UUID string
    val metricType: String, // 'hrv_rmssd' | 'resting_hr' | 'sleep_session' | ...
    val value: Double?,
    val unit: String?,
    val startTime: Long,
    val endTime: Long?,
    val zoneOffset: String?,
    val sourceApp: String,
    val sourceDevice: String?,
    val recordingMethod: String,
    val payload: String?,
    val clientSampleId: String,
    val priorityRank: Int,
    val confidence: Double,
    val createdAt: Long = System.currentTimeMillis()
)
