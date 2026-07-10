package com.example.atlethiq.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "daily_snapshots")
@Serializable
data class DailySnapshot(
    @PrimaryKey val day: String, // "YYYY-MM-DD"
    val call: String, // 'go' | 'hold' | 'back_off' | 'calibrating' | 'no_data'
    val signalScore: Int?,
    val confidence: String, // 'calibrating' | 'provisional' | 'reliable' | 'high'
    val decodeJson: String, // JSON payload representing factors
    val debugJson: String, // JSON payload representing engine debug info
    val engineVersion: String,
    val computedAt: Long = System.currentTimeMillis()
)
