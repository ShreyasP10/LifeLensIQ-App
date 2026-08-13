package com.lifelensiq.app.tracking

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.util.PermissionUtils
import kotlinx.coroutines.delay

/**
 * Polls UsageStatsManager every 15 s and emits APP_SESSION events
 * (start/end of foreground app usage).
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

    fun usageAccessGranted(): Boolean = PermissionUtils.isUsageAccessGranted(context)

    suspend fun pollLoop() {
        wasGranted = usageAccessGranted()
        while (true) {
            runCatching { pollOnce() }
            delay(POLL_INTERVAL_MS)
        }
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
        const val IDLE_CLOSE_MS = 90_000L
    }
}
