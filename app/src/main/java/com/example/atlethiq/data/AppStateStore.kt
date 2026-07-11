package com.example.atlethiq.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small persisted store backing the M4 daily-loop simulation.
 *
 * On mock data the entire 45-day seed is already present, so "a new day's data arriving" cannot be
 * modeled by real time. Instead [frontierDay] is a pointer to the app's current latest-with-data day
 * (1..45) — what Today opens on — and advancing it stands in for an overnight sync landing. The
 * WorkManager job reads this frontier, checks the real snapshot for that day, and posts the morning
 * Call notification at most once, guarded by [lastNotifiedDay]. [pendingNotifDay] carries the day a
 * tapped notification wants Today scoped to.
 */
@Singleton
class AppStateStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("atlethiq_app_state", Context.MODE_PRIVATE)

    /** The app's current latest-with-data day index (1..45), or -1 when never initialized. */
    var frontierDay: Int
        get() = prefs.getInt(KEY_FRONTIER, -1)
        set(value) = prefs.edit().putInt(KEY_FRONTIER, value).apply()

    /** Most recent day index a morning notification was posted for (0 = none). Duplicate guard. */
    var lastNotifiedDay: Int
        get() = prefs.getInt(KEY_LAST_NOTIFIED, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_NOTIFIED, value).apply()

    /** Day index a tapped notification wants Today scoped to (0 = none). Consumed once on resume. */
    var pendingNotifDay: Int
        get() = prefs.getInt(KEY_PENDING_NOTIF, 0)
        set(value) = prefs.edit().putInt(KEY_PENDING_NOTIF, value).apply()

    companion object {
        private const val KEY_FRONTIER = "frontier_day"
        private const val KEY_LAST_NOTIFIED = "last_notified_day"
        private const val KEY_PENDING_NOTIF = "pending_notif_day"

        const val DAY_MAX = 45
        private val DAY_MAX_DATE: LocalDate = LocalDate.parse("2026-07-10") // seed day 45

        /** Maps a 1..45 day index to its seed date string ("yyyy-MM-dd"). Day 45 = 2026-07-10. */
        fun dateForDay(index: Int): String =
            DAY_MAX_DATE.minusDays((DAY_MAX - index).toLong()).toString()

        /** Inverse of [dateForDay]. */
        fun dayForDate(date: String): Int =
            (DAY_MAX - ChronoUnit.DAYS.between(LocalDate.parse(date), DAY_MAX_DATE)).toInt()
    }
}
