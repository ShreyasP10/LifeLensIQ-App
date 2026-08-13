package com.lifelensiq.app.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.lifelensiq.app.domain.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Step counter via TYPE_STEP_COUNTER (cumulative since boot).
 * Emits STEPS events with the delta on each sensor update.
 * No-op if permission missing or sensor absent.
 */
class StepTracker(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var lastSteps: Int = 0
    private var registered = false
    private var emitterRef: EventEmitter? = null
    private var scopeRef: CoroutineScope? = null

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val cumulative = event.values[0].toInt()
            val delta = (cumulative - lastSteps).coerceAtLeast(0)
            if (lastSteps > 0 && delta > 0) {
                val emitter = emitterRef ?: return
                val scope = scopeRef ?: return
                scope.launch {
                    emitter.emit(
                        EventType.STEPS.id,
                        mapOf("stepDelta" to delta, "cumulativeSteps" to cumulative)
                    )
                }
            }
            lastSteps = cumulative
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start(scope: CoroutineScope, emitter: EventEmitter) {
        if (registered || sensor == null) return
        emitterRef = emitter
        scopeRef = scope
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        registered = true
    }

    fun stop() {
        if (registered) {
            sensorManager.unregisterListener(listener)
            registered = false
        }
        emitterRef = null
        scopeRef = null
    }
}
