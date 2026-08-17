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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Step counter via TYPE_STEP_COUNTER (cumulative since boot) with a
 * TYPE_STEP_DETECTOR fallback for devices without a step counter sensor.
 * Deltas are accumulated and emitted as one batched STEPS event every
 * FLUSH_INTERVAL_MS instead of one event per sensor tick.
 * No-op if permission missing or both sensors absent.
 */
class StepTracker(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val isDetectorFallback: Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) == null
    private val lock = Any()

    private var lastSteps: Int = 0
    private var pendingDelta: Int = 0
    private var lastCumulative: Int = 0
    private var registered = false
    private var emitterRef: EventEmitter? = null
    private var scopeRef: CoroutineScope? = null

    /** True while the sensor listener is registered. */
    var isRunning: Boolean = false
        private set

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            synchronized(lock) {
                if (isDetectorFallback) {
                    // Each STEP_DETECTOR event is exactly one step.
                    pendingDelta += 1
                    lastCumulative += 1
                } else {
                    val cumulative = event.values[0].toInt()
                    val delta = (cumulative - lastSteps).coerceAtLeast(0)
                    if (lastSteps > 0 && delta > 0) {
                        pendingDelta += delta
                        lastCumulative = cumulative
                    }
                    lastSteps = cumulative
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start(scope: CoroutineScope, emitter: EventEmitter) {
        if (registered || sensor == null) return
        emitterRef = emitter
        scopeRef = scope
        val ok = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        if (!ok) return
        registered = true
        isRunning = true
        scope.launch {
            while (registered) {
                delay(FLUSH_INTERVAL_MS)
                flush()
            }
        }
    }

    fun stop() {
        flush()
        if (registered) {
            sensorManager.unregisterListener(listener)
            registered = false
            isRunning = false
        }
        emitterRef = null
        scopeRef = null
    }

    private fun flush() {
        val emitter = emitterRef ?: return
        val scope = scopeRef ?: return
        val (delta, cumulative) = synchronized(lock) {
            val d = pendingDelta
            pendingDelta = 0
            d to lastCumulative
        }
        if (delta > 0) {
            scope.launch {
                emitter.emit(
                    EventType.STEPS.id,
                    mapOf("stepDelta" to delta, "cumulativeSteps" to cumulative)
                )
            }
        }
    }

    companion object {
        const val FLUSH_INTERVAL_MS = 10 * 60 * 1000L
    }
}