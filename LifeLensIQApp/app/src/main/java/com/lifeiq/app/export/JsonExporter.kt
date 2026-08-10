package com.lifeiq.app.export

import com.lifeiq.app.data.local.EventEntity
import com.lifeiq.app.util.JsonUtil
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * JSON exporter — envelope with events (payloads parsed to objects),
 * timetable, and metadata. NDJSON variant streams one event per line.
 */
class JsonExporter(private val ndjson: Boolean = false) : Exporter {

    override fun write(events: List<EventEntity>, timetable: List<Map<String, Any?>>, out: OutputStream) {
        val writer = OutputStreamWriter(out, StandardCharsets.UTF_8)

        if (ndjson) {
            events.forEach { event ->
                writer.write(eventToJson(event).toString())
                writer.write("\n")
            }
            writer.flush()
            return
        }

        val envelope = buildJsonObject {
            put("exportedAt", kotlinx.serialization.json.JsonPrimitive(
                java.time.Instant.now().toString()
            ))
            put("schemaVersion", 1)
            put("count", events.size)
            putJsonArray("events") {
                events.forEach { add(eventToJson(it)) }
            }
            putJsonArray("timetable") {
                timetable.forEach { slot ->
                    add(JsonUtil.decodePayload(JsonUtil.encodePayload(slot)))
                }
            }
        }
        writer.write(envelope.toString())
        writer.flush()
    }

    private fun eventToJson(event: EventEntity): JsonObject = buildJsonObject {
        put("eventId", event.eventId)
        put("userId", event.userId)
        put("deviceId", event.deviceId)
        put("eventType", event.eventType)
        put("schemaVersion", event.schemaVersion)
        put("timestamp", event.timestamp)
        put("payload", JsonUtil.decodePayload(event.payloadJson))
    }
}
