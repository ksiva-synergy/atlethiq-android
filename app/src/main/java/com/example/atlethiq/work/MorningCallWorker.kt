package com.example.atlethiq.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.atlethiq.data.AppStateStore
import com.example.atlethiq.data.DataRepository
import com.example.atlethiq.notifications.CallNotifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Fires the daily morning Call notification when a new snapshot becomes available for the app's
 * current frontier day (execution-plan-v2 §B M4).
 *
 * It never fires blind: it reads the real persisted snapshot for the frontier day and posts only if
 * a Call (go/hold/back_off) actually exists. It never fires twice: [AppStateStore.lastNotifiedDay]
 * gates re-posts, so reopening the app on an already-notified day is a no-op. Dependencies are
 * pulled through a Hilt [EntryPoint] so the default WorkManager factory can construct the worker.
 */
class MorningCallWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun dataRepository(): DataRepository
        fun appStateStore(): AppStateStore
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val store = deps.appStateStore()
        val repo = deps.dataRepository()

        val frontier = store.frontierDay
        val lastNotified = store.lastNotifiedDay
        Log.i(TAG, "run start: frontierDay=$frontier lastNotifiedDay=$lastNotified")

        if (frontier < 1) {
            Log.i(TAG, "frontier uninitialized — skip")
            return Result.success()
        }
        if (frontier <= lastNotified) {
            Log.i(TAG, "no new day beyond lastNotified ($frontier <= $lastNotified) — skip, not re-firing")
            return Result.success()
        }

        val dateStr = AppStateStore.dateForDay(frontier)
        val snapshot = repo.getDailySnapshotSync(dateStr)
        if (snapshot == null) {
            Log.i(TAG, "no snapshot in local store for day $frontier ($dateStr) — skip (never fire blind)")
            return Result.success()
        }
        if (snapshot.call !in REAL_CALLS) {
            Log.i(TAG, "snapshot for day $frontier ($dateStr) is call=${snapshot.call} — no Call issued, skip")
            return Result.success()
        }

        Log.i(TAG, "new data detected for day $frontier ($dateStr) call=${snapshot.call} — posting notification")
        CallNotifier.postCall(applicationContext, snapshot.call, frontier)
        store.lastNotifiedDay = frontier
        Log.i(TAG, "notification posted; lastNotifiedDay advanced to $frontier")
        return Result.success()
    }

    companion object {
        const val TAG = "MorningCallWorker"
        private val REAL_CALLS = setOf("go", "hold", "back_off")

        /** Enqueues a one-shot run of the morning-Call check. */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<MorningCallWorker>().build())
        }
    }
}
