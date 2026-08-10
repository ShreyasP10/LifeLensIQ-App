package com.lifeiq.app.tracking

import com.lifeiq.app.domain.EventType

/**
 * Pure logic: if screen-on happens after > 5h idle, emit a WAKE_UP event
 * carrying the inferred sleep window. Injected clock for testability.
 */
class WakeDetector(
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val idleThresholdMs: Long = IDLE_THRESHOLD_MS
) {

    private var lastScreenOffMs: Long? = null

    fun onScreenOff() {
        lastScreenOffMs = nowMs()
    }

    suspend fun onScreenOn(emitter: EventEmitter): Boolean {
        val lastOff = lastScreenOffMs ?: return false
        val now = nowMs()
        val idle = now - lastOff
        if (idle >= idleThresholdMs) {
            emitter.emit(
                EventType.WAKE_UP.id,
                mapOf(
                    "sleptFromMs" to lastOff,
                    "sleptToMs" to now,
                    "durationMs" to idle
                )
            )
            return true
        }
        return false
    }

    companion object {
        const val IDLE_THRESHOLD_MS = 5 * 60 * 60 * 1000L // 5 hours
    }
}
