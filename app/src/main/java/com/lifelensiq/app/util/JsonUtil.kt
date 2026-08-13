package com.lifelensiq.app.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Converts arbitrary payload maps to/from JSON strings (Room payloadJson column). */
object JsonUtil {

    val json = Json { ignoreUnknownKeys = true }

    fun encodePayload(payload: Map<String, Any?>): String =
        buildJsonObject {
            payload.forEach { (k, v) -> put(k, v.toJsonElement()) }
        }.toString()

    fun decodePayload(raw: String): JsonObject = runCatching {
        json.parseToJsonElement(raw) as JsonObject
    }.getOrDefault(JsonObject(emptyMap()))

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Double -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(this.entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
        is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

    /** Flatten a payload map for Firestore (nulls removed; Firestore rejects null). */
    fun forFirestore(payload: Map<String, Any?>): Map<String, Any?> =
        payload.filterValues { it != null }
}
