package com.lifelensiq.app.tracking

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.util.PermissionUtils
import kotlinx.coroutines.delay

/**
 * Polls UsageStatsManager and emits APP_SESSION events
 * (start/end of foreground app usage). Polls every 15 s while the
 * screen is on and relaxes to 60 s while it is off to save battery.
 */
class AppUsagePoller(
    private val context: Context,
    private val emitter: EventEmitter
) {

    private var currentPackage: String? = null
    private var currentStart: Long = 0L
    private var wasGranted: Boolean = true

    private val usm: UsageStatsManager
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val powerManager: PowerManager
        get() = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun usageAccessGranted(): Boolean = PermissionUtils.isUsageAccessGranted(context)

    suspend fun pollLoop() {
        wasGranted = usageAccessGranted()
        while (true) {
            if (isNight()) {
                // Night pause: no polling 00:00-06:00 — battery saver.
                closeCurrentIfIdle()
                delay(timeUntilMorningMs())
                continue
            }
            runCatching { pollOnce() }
            delay(pollIntervalMs())
        }
    }

    /** 15 s while the screen is on, 60 s while off. */
    private fun pollIntervalMs(): Long =
        if (powerManager.isInteractive) POLL_INTERVAL_MS else IDLE_POLL_INTERVAL_MS

    private fun isNight(): Boolean {
        val hour = java.time.LocalTime.now().hour
        return hour < NIGHT_PAUSE_END_HOUR
    }

    /** Millis until 06:00 today (or tomorrow if past midnight... always today when hour < 6). */
    private fun timeUntilMorningMs(): Long {
        val now = java.time.ZonedDateTime.now()
        val target = now.toLocalDate().atTime(NIGHT_PAUSE_END_HOUR, 0).atZone(now.zone)
        return java.time.Duration.between(now, target).toMillis().coerceAtLeast(1)
    }

    suspend fun pollOnce() {
        if (!usageAccessGranted()) {
            closeCurrentIfIdle()
            if (wasGranted) {
                wasGranted = false
                emitter.emit(
                    EventType.TRACKING_STATE.id,
                    mapOf("state" to "PAUSED", "reason" to "PERMISSION_REVOKED")
                )
            }
            return
        }
        if (!wasGranted) {
            wasGranted = true
            emitter.emit(EventType.TRACKING_STATE.id, mapOf("state" to "STARTED", "reason" to "PERMISSION_GRANTED"))
        }
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - POLL_INTERVAL_MS - 1000, now)
        val e = UsageEvents.Event()
        var latestForeground: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                latestForeground = e.packageName
            }
        }
        val candidate = latestForeground?.takeIf { pkg -> !isSystem(pkg) }

        if (candidate == null) {
            // No foreground app observed this window.
            if (currentPackage != null && now - currentStart > IDLE_CLOSE_MS) {
                closeCurrent(now)
            }
            return
        }
        if (candidate != currentPackage) {
            closeCurrent(now)
            currentPackage = candidate
            currentStart = now
        }
        if (candidate != null) checkFocusBlock(candidate)
    }

    /** Focus mode: pull the user back when a blocked app comes to foreground. */
    private fun checkFocusBlock(pkg: String) {
        if (!com.lifelensiq.app.util.SettingsStore.focusActive) return
        if (FocusBlockActivity.isShowing) return
        if (pkg == context.packageName) return
        if (pkg !in com.lifelensiq.app.util.SettingsStore.focusBlockedApps()) return
        runCatching {
            val intent = Intent(context, FocusBlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(intent)
        }
    }

    private suspend fun closeCurrent(now: Long) {
        val pkg = currentPackage ?: return
        currentPackage = null
        emitter.emit(
            EventType.APP_SESSION.id,
            mapOf(
                "packageName" to pkg,
                "appName" to appName(pkg),
                "startedAt" to currentStart,
                "endedAt" to now,
                "durationMs" to (now - currentStart)
            )
        )
    }

    private suspend fun closeCurrentIfIdle() {
        val now = System.currentTimeMillis()
        if (currentPackage != null) closeCurrent(now)
    }

    private fun isSystem(pkg: String): Boolean {
        if (pkg == "android") return true
        return runCatching {
            val pm = context.packageManager
            val app = pm.getApplicationInfo(pkg, 0)
            app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 &&
                app.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
        }.getOrDefault(false)
    }

    private fun appName(pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    companion object {
        const val POLL_INTERVAL_MS = 15_000L
        const val IDLE_POLL_INTERVAL_MS = 60_000L
        const val IDLE_CLOSE_MS = 90_000L
        const val NIGHT_PAUSE_END_HOUR = 6
    }
}
