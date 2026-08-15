package com.lifelensiq.app.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Converts JsonElement trees to plain Kotlin objects (Firestore-compatible). */
fun JsonElement.toPlain(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull
        else -> content.toLongOrNull() ?: content.toDoubleOrNull() ?: content
    }
    is JsonObject -> mapValues { (_, v) -> v.toPlain() }
    is JsonArray -> map { it.toPlain() }
    else -> null
}
