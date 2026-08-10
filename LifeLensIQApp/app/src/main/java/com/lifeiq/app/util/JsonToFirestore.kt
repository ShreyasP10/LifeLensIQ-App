package com.lifeiq.app.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Converts JsonElement trees to plain Kotlin objects (Firestore-compatible). */
fun JsonElement.toPlain(): Any? = when (this) {
    is JsonPrimitive -> when {
        isString -> content
        content == "true" -> true
        content == "false" -> false
        content.toLongOrNull() != null -> content.toLong()
        content.toDoubleOrNull() != null -> content.toDouble()
        else -> content
    }
    is JsonObject -> this.mapValues { (_, v) -> v.toPlain() }
    is JsonArray -> map { it.toPlain() }
    else -> null
}
