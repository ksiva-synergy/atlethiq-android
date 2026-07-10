package com.example.atlethiq.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.atlethiq.data.models.CallFeedback
import kotlinx.coroutines.flow.Flow

@Dao
interface CallFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallFeedback(feedback: CallFeedback): Long

    @Query("SELECT * FROM call_feedback ORDER BY day DESC")
    fun getAllCallFeedbackFlow(): Flow<List<CallFeedback>>
}
