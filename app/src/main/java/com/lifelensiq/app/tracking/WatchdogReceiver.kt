package com.lifelensiq.app.tracking

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

/**
 * Periodic watchdog that ensures the tracker service is running.
 * Schedules itself every 15 minutes via AlarmManager.
 */
class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val serviceRunning = isServiceRunning(context)
        if (!serviceRunning) {
            LifeLensIQTrackerService.start(context)
        }
        // Reschedule the next check
        scheduleNextCheck(context)
    }

    private fun isServiceRunning(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return am.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == LifeLensIQTrackerService::class.java.name
        }
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
        private const val ACTION_WATCHDOG = "com.lifelensiq.app.WATCHDOG_CHECK"

        fun scheduleNextCheck(context: Context) {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WatchdogReceiver::class.java).setAction(ACTION_WATCHDOG)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
            // Use setAndAllowWhileIdle (no SCHEDULE_EXACT_ALARM permission needed).
            // Exact alarms require a permission on API 31+ and would throw there.
            alarmMgr.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + CHECK_INTERVAL_MS,
                pendingIntent
            )
        }

        fun cancel(context: Context) {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WatchdogReceiver::class.java).setAction(ACTION_WATCHDOG)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
            alarmMgr.cancel(pendingIntent)
        }
    }
}