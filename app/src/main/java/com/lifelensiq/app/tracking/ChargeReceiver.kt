package com.lifelensiq.app.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.lifelensiq.app.domain.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChargeReceiver(private val scope: CoroutineScope) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val now = System.currentTimeMillis()
        val emitter = com.lifelensiq.app.di.ServiceLocator.eventEmitter()
        val batteryPct = (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val type = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                scope.launch {
                    emitter.emit(
                        EventType.CHARGE_START.id,
                        mapOf("plugType" to plugName(type), "batteryPct" to batteryPct)
                    )
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                scope.launch {
                    emitter.emit(EventType.CHARGE_END.id, mapOf("batteryPct" to batteryPct))
                }
            }
        }
    }

    private fun plugName(type: Int): String = when (type) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
        else -> "UNKNOWN"
    }

    fun intentFilter(): IntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
    }
}
