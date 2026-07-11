package com.example.atlethiq.data.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Entity(
    tableName = "metric_samples",
    indices = [
        Index(value = ["metricType", "clientSampleId"], unique = true)
    ]
)
@Serializable
data class MetricSample(
    @PrimaryKey val id: String, // UUID string
    @SerialName("metric_type") val metricType: String, // 'hrv_rmssd' | 'resting_hr' | 'sleep_session' | ...
    val value: Double?,
    val unit: String?,
    @SerialName("start_time") val startTimeString: String,
    @SerialName("end_time") val endTimeString: String? = null,
    @SerialName("zone_offset") val zoneOffset: String?,
    @SerialName("source_app") val sourceApp: String,
    @SerialName("source_device") val sourceDevice: String?,
    @SerialName("recording_method") val recordingMethod: String,
    @SerialName("client_sample_id") val clientSampleId: String,
    @SerialName("created_at") val createdAtString: String? = null,
    // Local Room columns not in Supabase API
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val priorityRank: Int = 0,
    val confidence: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val payloadJson: String = ""
) {
    @Ignore val payload: JsonElement? = null
}
