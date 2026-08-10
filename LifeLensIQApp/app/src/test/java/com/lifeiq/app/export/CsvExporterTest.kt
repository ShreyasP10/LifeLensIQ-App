package com.lifeiq.app.export

import com.lifeiq.app.data.local.EventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class CsvExporterTest {

    private val exporter = CsvExporter()

    private fun event(
        id: String = "evt-1",
        type: String = "APP_SESSION",
        payload: String = """{"packageName":"com.instagram.android","appName":"Instagram","durationMs":600000}""",
        timestamp: Long = 1_789_000_000_000L
    ) = EventEntity(
        eventId = id,
        userId = "u1",
        deviceId = "d1",
        eventType = type,
        payloadJson = payload,
        timestamp = timestamp,
        synced = false
    )

    @Test
    fun `writes BOM and header`() {
        val out = ByteArrayOutputStream()
        exporter.write(listOf(event()), emptyList(), out)
        val text = out.toString(StandardCharsets.UTF_8.name())
        assertTrue(text.startsWith("\uFEFF"))
        assertTrue(text.contains("schemaVersion,eventId,userId"))
    }

    @Test
    fun `escapes commas and quotes in payload`() {
        val out = ByteArrayOutputStream()
        exporter.write(listOf(event(payload = """{"note":"a,b\"c","x":1}""")), emptyList(), out)
        val text = out.toString(StandardCharsets.UTF_8.name())
        assertTrue(text.contains("\"\"") || text.contains("\"a,b\"\"c\""))
    }

    @Test
    fun `writes one row per event`() {
        val events = (1..5).map { event(id = "evt-$it") }
        val out = ByteArrayOutputStream()
        exporter.write(events, emptyList(), out)
        val lines = out.toString(StandardCharsets.UTF_8.name()).split("\r\n").filter { it.isNotBlank() }
        assertEquals(6, lines.size) // header + 5
    }
}
