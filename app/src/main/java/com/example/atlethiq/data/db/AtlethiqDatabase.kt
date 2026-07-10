package com.example.atlethiq.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.atlethiq.data.models.CallFeedback
import com.example.atlethiq.data.models.DailySnapshot
import com.example.atlethiq.data.models.MetricSample

@Database(
    entities = [
        MetricSample::class,
        DailySnapshot::class,
        CallFeedback::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AtlethiqDatabase : RoomDatabase() {
    abstract fun metricSampleDao(): MetricSampleDao
    abstract fun dailySnapshotDao(): DailySnapshotDao
    abstract fun callFeedbackDao(): CallFeedbackDao
}
