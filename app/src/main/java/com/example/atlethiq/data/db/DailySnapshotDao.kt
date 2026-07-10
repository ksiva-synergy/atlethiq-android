package com.example.atlethiq.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.atlethiq.data.models.DailySnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySnapshot(snapshot: DailySnapshot): Long

    @Query("SELECT * FROM daily_snapshots WHERE day = :day LIMIT 1")
    fun getDailySnapshotFlow(day: String): Flow<DailySnapshot?>

    @Query("SELECT * FROM daily_snapshots ORDER BY day DESC")
    fun getAllDailySnapshotsFlow(): Flow<List<DailySnapshot>>
}
