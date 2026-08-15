package com.lifelensiq.app.util

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JsonUtilTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun toPlain_keepsNumericLookingStringsAsStrings() {
        val element = json.parseToJsonElement("\"21\"")
        assertEquals("21", element.toPlain())
    }

    @Test
    fun toPlain_convertsRealNumbersAndBooleans() {
        val plain = json.parseToJsonElement("""{"a": 21, "b": 1.5, "c": true}""").toPlain() as Map<*, *>
        assertEquals(21L, plain["a"])
        assertEquals(1.5, plain["b"])
        assertEquals(true, plain["c"])
    }

    @Test
    fun forFirestore_stripsNullsRecursively() {
        val cleaned = JsonUtil.forFirestore(
            mapOf(
                "a" to null,
                "b" to 1L,
                "c" to mapOf("x" to null, "y" to "keep"),
                "d" to listOf(null, "keep", null)
            )
        )
        assertFalse(cleaned.containsKey("a"))
        assertEquals(mapOf("y" to "keep"), cleaned["c"])
        assertEquals(listOf("keep"), cleaned["d"])
    }

    @Test
    fun forFirestore_dropsArraysThatBecomeEmpty() {
        val cleaned = JsonUtil.forFirestore(mapOf("a" to listOf(null)))
        assertFalse(cleaned.containsKey("a"))
    }
}
