package com.example.atlethiq.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "call_feedback")
@Serializable
data class CallFeedback(
    @PrimaryKey val id: String, // UUID string
    val day: String, // "YYYY-MM-DD"
    val issuedCall: String,
    val userVerdict: String,
    val reasons: String, // Comma-separated or JSON list of reasons
    val note: String?,
    val createdAt: Long = System.currentTimeMillis()
)
