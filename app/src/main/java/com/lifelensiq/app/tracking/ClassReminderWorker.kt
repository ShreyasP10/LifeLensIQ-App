package com.lifelensiq.app.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifelensiq.app.MainActivity
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.model.SlotType
import com.lifelensiq.app.util.TimeUtils
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Periodically checks today's timetable and posts a "class starts soon /
 * class in progress" notification for each attendable slot (FR-4.1).
 * Dedupes per slot per day via SharedPreferences.
 */
class ClassReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runCatching {
            val today = TimeUtils.todayName()
            val slots = ServiceLocator.timetableRepository().observeDay(today).first()
                .filter { it.applicable && it.type in SlotType.ATTENDABLE }
            if (slots.isEmpty()) return Result.success()

            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val dateKey = LocalDate.now().toString()
            val now = LocalTime.now()

            slots.forEach { slot ->
                val start = TimeUtils.parse(slot.start)
                val minutesToStart = Duration.between(now, start).toMinutes()
                if (minutesToStart in -15..15) {
                    val key = "$dateKey:${slot.day}:${slot.slotNo}"
                    if (!prefs.getBoolean(key, false)) {
                        notifyClass(slot.subjectFull, slot.start, slot.room, minutesToStart >= 0)
                        prefs.edit().putBoolean(key, true).apply()
                    }
                }
            }
        }
        return Result.success()
    }

    private fun notifyClass(subject: String, start: String, room: String, upcoming: Boolean) {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Class reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        val openApp = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java)
                .apply { putExtra("route", "attendance") },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(if (upcoming) "Class starts soon — $subject" else "Class in progress — $subject")
            .setContentText("$start · Room ${room.ifBlank { "—" }} · Tap to mark attendance")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { nm.notify(subject.hashCode() and 0x7fffffff, notification) }
    }

    companion object {
        const val WORK_NAME = "lifeiq_class_reminders"
        private const val CHANNEL_ID = "lifeiq_classes"
        private const val PREFS = "lifeiq_class_reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ClassReminderWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
