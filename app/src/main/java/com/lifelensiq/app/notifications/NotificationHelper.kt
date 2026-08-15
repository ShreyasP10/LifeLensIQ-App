package com.lifelensiq.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lifelensiq.app.MainActivity
import com.lifelensiq.app.R

/** Posts insight notifications (daily summary, limit alerts, reminders). */
object NotificationHelper {

    const val CHANNEL_SUMMARY = "lifelensiq_summary"
    const val CHANNEL_ALERTS = "lifelensiq_alerts"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SUMMARY,
                "Daily summaries",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Limits & reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    fun post(
        context: Context,
        channelId: String,
        id: Int,
        title: String,
        body: String
    ) {
        val openIntent = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        runCatching {
            context.getSystemService(NotificationManager::class.java).notify(id, notification)
        }
    }
}