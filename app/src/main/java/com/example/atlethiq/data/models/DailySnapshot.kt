package com.example.atlethiq.data.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Entity(tableName = "daily_snapshots")
@Serializable
data class DailySnapshot(
    @PrimaryKey val day: String, // "YYYY-MM-DD"
    val call: String, // 'go' | 'hold' | 'back_off' | 'calibrating' | 'no_data'
    @SerialName("signal_score") val signalScore: Int?,
    val confidence: String, // 'calibrating' | 'provisional' | 'reliable' | 'high'
    @SerialName("engine_version") val engineVersion: String,
    @SerialName("computed_at") val computedAt: String? = null,
    val decodeJson: String = "",
    val debugJson: String = ""
) {
    @Ignore @SerialName("decode") val decode: JsonElement? = null
    @Ignore @SerialName("debug") val debug: JsonElement? = null
}
