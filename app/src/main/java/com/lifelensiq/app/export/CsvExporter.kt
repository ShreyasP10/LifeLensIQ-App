package com.lifelensiq.app.export

import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.util.JsonUtil
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CSV exporter — UTF-8 with BOM for Excel compatibility.
 * Events flattened: one row per event with payload in payload_json column.
 */
class CsvExporter : Exporter {

    override fun write(events: List<EventEntity>, timetable: List<Map<String, Any?>>, out: OutputStream) {
        val writer = OutputStreamWriter(out, StandardCharsets.UTF_8)
        writer.write("\uFEFF") // BOM for Excel
        writer.write(HEADER.joinToString(","))
        writer.write("\r\n")
        events.forEach { event ->
            val payload = JsonUtil.decodePayload(event.payloadJson)
            val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
            )
            val row = listOf(
                event.schemaVersion.toString(),
                event.eventId,
                event.userId,
                event.deviceId,
                event.eventType,
                event.timestamp.toString(),
                iso,
                payload.toString()
            )
            writer.write(row.joinToString(",") { escape(it) })
            writer.write("\r\n")
        }
        writer.flush()
    }

    private fun escape(value: String): String {
        val needsQuote = value.contains(",") || value.contains("\"") || value.contains("\n")
        return if (needsQuote) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }

    companion object {
        private val HEADER = listOf(
            "schemaVersion", "eventId", "userId", "deviceId", "eventType",
            "timestamp_utc_ms", "timestamp_iso_local", "payload_json"
        )
    }
}
