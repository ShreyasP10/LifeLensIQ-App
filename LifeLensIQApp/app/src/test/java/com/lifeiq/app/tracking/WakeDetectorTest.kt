package com.lifeiq.app.tracking

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeDetectorTest {

    private val emitter = FakeEmitter()

    @Test
    fun `emits wake up after long idle`() = runTest {
        var now = 1_000_000L
        val detector = WakeDetector(nowMs = { now }, idleThresholdMs = 5 * 60 * 60 * 1000L)
        detector.onScreenOff()
        now += 6 * 60 * 60 * 1000L // 6h later
        val emitted = detector.onScreenOn(emitter)
        assertTrue(emitted)
        assertTrue(emitter.events.any { it.first == "WAKE_UP" })
    }

    @Test
    fun `does not emit on short idle`() = runTest {
        var now = 1_000_000L
        val detector = WakeDetector(nowMs = { now })
        detector.onScreenOff()
        now += 10 * 60 * 1000L // 10 min
        assertFalse(detector.onScreenOn(emitter))
        assertTrue(emitter.events.isEmpty())
    }

    @Test
    fun `does not emit when never saw screen off`() = runTest {
        val detector = WakeDetector(nowMs = { 2_000_000L })
        assertFalse(detector.onScreenOn(emitter))
    }

    private class FakeEmitter : EventEmitter {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        override suspend fun emit(eventType: String, payload: Map<String, Any?>): String {
            events += eventType to payload
            return "id-${events.size}"
        }
    }
}
