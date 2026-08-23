package com.lifelensiq.app.tracking

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.PowerManager
import android.util.Log
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.util.PermissionUtils
import kotlinx.coroutines.delay

/**
 * Polls UsageStatsManager and emits APP_SESSION events
 * (start/end of foreground app usage). Runs 24/7: polls every 15 s while
 * the screen is on and relaxes to 60 s while it is off to save battery.
 * Sessions end exactly on MOVE_TO_BACKGROUND / screen-off.
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

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lifelensiq_tracker", Context.MODE_PRIVATE)

    private val TAG = "AppUsagePoller"

    init {
        restoreState()
    }

    fun usageAccessGranted(): Boolean = PermissionUtils.isUsageAccessGranted(context)

    suspend fun pollLoop() {
        wasGranted = usageAccessGranted()
        Log.d(TAG, "Starting poll loop, granted=$wasGranted")
        while (true) {
            runCatching { pollOnce() }.onFailure { e ->
                Log.e(TAG, "pollOnce failed", e)
            }
            delay(pollIntervalMs())
        }
    }

    /** 15 s while the screen is on, 60 s while off. */
    private fun pollIntervalMs(): Long =
        if (powerManager.isInteractive) POLL_INTERVAL_MS else IDLE_POLL_INTERVAL_MS

    suspend fun pollOnce() {
        if (!usageAccessGranted()) {
            closeCurrentIfIdle()
            if (wasGranted) {
                wasGranted = false
                Log.w(TAG, "Usage access revoked")
                emitter.emit(
                    EventType.TRACKING_STATE.id,
                    mapOf("state" to "PAUSED", "reason" to "PERMISSION_REVOKED")
                )
            }
            return
        }
        if (!wasGranted) {
            wasGranted = true
            Log.i(TAG, "Usage access granted")
            emitter.emit(EventType.TRACKING_STATE.id, mapOf("state" to "STARTED", "reason" to "PERMISSION_GRANTED"))
        }
        val now = System.currentTimeMillis()

        // Screen off → the device is not in use, the current session is over.
        if (!powerManager.isInteractive) {
            closeCurrent(now)
            return
        }

        val events = usm.queryEvents(now - POLL_INTERVAL_MS - 1000, now) ?: return
        val e = UsageEvents.Event()
        var latestForeground: Pair<String, Long>? = null
        var currentBackgroundedAt: Long? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> latestForeground = e.packageName to e.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (e.packageName == currentPackage) currentBackgroundedAt = e.timeStamp
                }
            }
        }

        if (currentBackgroundedAt != null) {
            // The tracked app went to the background in this window — end the
            // session exactly there instead of waiting for a new foreground.
            Log.d(TAG, "Background event for $currentPackage at $currentBackgroundedAt")
            closeCurrent(currentBackgroundedAt)
            return
        }

        val fg = latestForeground
        if (fg != null) {
            val candidate = fg.first.takeIf { pkg -> !isSystem(pkg) }
            if (candidate == null) {
                // Foreground switched to a system app (launcher/home) → session ended.
                Log.d(TAG, "System app foreground, closing session")
                closeCurrent(now)
                return
            }
            if (candidate != currentPackage) {
                Log.d(TAG, "App switch: $currentPackage -> $candidate")
                closeCurrent(now)
                currentPackage = candidate
                currentStart = fg.second
                saveState()
            }
            checkFocusBlock(candidate)
            return
        }

        // No app switch in this window. If we already track an app, keep the
        // session open — the user may have stayed in it for hours.
        if (currentPackage != null) return

        // Bootstrap: find the currently-foreground app from aggregated stats
        // so the very first session after grant is captured too.
        val recent = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, now - BOOTSTRAP_WINDOW_MS, now
        ).filter { it.lastTimeUsed >= now - BOOTSTRAP_WINDOW_MS && !isSystem(it.packageName) }
            .maxByOrNull { it.lastTimeUsed }
        if (recent != null) {
            Log.d(TAG, "Bootstrap session: ${recent.packageName} at ${recent.lastTimeUsed}")
            currentPackage = recent.packageName
            currentStart = recent.lastTimeUsed
            saveState()
        }
    }

    /** Focus mode: pull the user back when a blocked app comes to foreground. */
    private fun checkFocusBlock(pkg: String) {
        if (!com.lifelensiq.app.util.SettingsStore.focusActive) return
        if (FocusBlockActivity.isShowing.get()) return
        if (pkg == context.packageName) return
        if (pkg !in com.lifelensiq.app.util.SettingsStore.focusBlockedApps()) return
        if (!FocusBlockActivity.isShowing.compareAndSet(false, true)) return
        runCatching {
            val intent = Intent(context, FocusBlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(intent)
        }
    }

    private suspend fun closeCurrent(now: Long) {
        val pkg = currentPackage ?: return
        currentPackage = null
        currentStart = 0L
        clearState()
        Log.d(TAG, "Closing session for $pkg, duration=${now - currentStart}ms")
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

    private fun saveState() {
        prefs.edit()
            .putString("current_package", currentPackage)
            .putLong("current_start", currentStart)
            .apply()
    }

    private fun restoreState() {
        currentPackage = prefs.getString("current_package", null)
        currentStart = prefs.getLong("current_start", 0L)
        if (currentPackage != null) {
            Log.d(TAG, "Restored session: $currentPackage since $currentStart")
        }
    }

    private fun clearState() {
        prefs.edit()
            .remove("current_package")
            .remove("current_start")
            .apply()
    }

    companion object {
        const val POLL_INTERVAL_MS = 15_000L
        const val IDLE_POLL_INTERVAL_MS = 60_000L
        const val BOOTSTRAP_WINDOW_MS = 3 * 60_000L
    }
}