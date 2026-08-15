package com.lifelensiq.app.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lifelensiq.app.domain.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives screen on/off and unlock broadcasts, emits events, and feeds
 * the wake detector (first screen-on after >5h idle => WAKE_UP).
 */
class ScreenStateReceiver(private val scope: CoroutineScope) : BroadcastReceiver() {

    var onScreenOn: (suspend (EventEmitter) -> Unit)? = null
    var onScreenOff: (() -> Unit)? = null

    private var lastScreenOff: Long = System.currentTimeMillis()

    override fun onReceive(context: Context, intent: Intent) {
        val now = System.currentTimeMillis()
        val emitter = com.lifelensiq.app.di.ServiceLocator.eventEmitter()
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                val idle = now - lastScreenOff
                scope.launch {
                    emitter.emit(EventType.SCREEN_ON.id, mapOf("precedingIdleMs" to idle))
                    onScreenOn?.invoke(emitter)
                    com.lifelensiq.app.notifications.MorningReporter.maybePost(context)
                }
            }
            Intent.ACTION_SCREEN_OFF -> {
                lastScreenOff = now
                onScreenOff?.invoke()
                scope.launch { emitter.emit(EventType.SCREEN_OFF.id, mapOf("timestamp" to now)) }
            }
            Intent.ACTION_USER_PRESENT -> {
                scope.launch { emitter.emit(EventType.UNLOCK.id, mapOf("timestamp" to now)) }
            }
        }
    }

    fun intentFilter(): IntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_USER_PRESENT)
    }
}
