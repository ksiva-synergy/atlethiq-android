package com.example.atlethiq.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.atlethiq.MainActivity
import com.example.atlethiq.R

/**
 * Builds and posts the single daily "Call" notification (design spec §6).
 *
 * One guaranteed push per day, one variant per Call state — small monogram icon, no image, no
 * actions except opening the app to Today for that day. A fixed [NOTIFICATION_ID] means a later
 * post replaces any earlier one, so at most one is ever visible.
 */
object CallNotifier {

    const val CHANNEL_ID = "atlethiq_daily_call"
    const val NOTIFICATION_ID = 1001
    const val EXTRA_NOTIF_DAY = "atlethiq.notif.day"

    private const val CHANNEL_NAME = "The Call"
    private const val CHANNEL_DESC = "One daily push when your morning Call lands."

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = CHANNEL_DESC }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts the Call notification for [call] ("go" | "hold" | "back_off"), scoped to [dayIndex].
     * Title carries the verbatim Call verb; body is the one-line Decode summary for that state.
     * Accent color is bound to the state's ink. No-op for any non-Call state.
     */
    fun postCall(context: Context, call: String, dayIndex: Int) {
        val content = contentFor(call) ?: return
        ensureChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIF_DAY, dayIndex)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            dayIndex, // distinct request code per day so the extra is preserved
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_monogram)
            .setColor(content.inkColor)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private data class CallContent(val title: String, val body: String, val inkColor: Int)

    private fun contentFor(call: String): CallContent? = when (call) {
        "go" -> CallContent(
            title = "The Call: Go hard.",
            body = "You're recovered and trending up. Today's the day to push.",
            inkColor = INK_LIME
        )
        "hold" -> CallContent(
            title = "The Call: Hold.",
            body = "You're steady, not surging. Train — but don't chase a peak today.",
            inkColor = INK_CYAN
        )
        "back_off" -> CallContent(
            title = "The Call: Back off.",
            body = "Recovery hasn't caught up with this week's load. Easy day — details inside.",
            inkColor = INK_ORANGE
        )
        else -> null
    }

    // Ink hexes mirror ui/theme/Color.kt (notifications are framework views, outside Compose theme).
    private const val INK_LIME = 0xFFC6F03C.toInt()
    private const val INK_CYAN = 0xFF3CE0F0.toInt()
    private const val INK_ORANGE = 0xFFFF7A1A.toInt()
}
