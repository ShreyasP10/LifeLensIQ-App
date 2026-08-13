package com.lifelensiq.app.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableImporterTest {

    private val importer = TimetableImporter()

    @Test
    fun `parses valid personalized timetable`() {
        val result = importer.parse(VALID_JSON)
        assertTrue(result.errors.isEmpty())
        assertEquals(8, result.slots.size)
        assertEquals("IP", result.slots[0].subject)
        assertEquals("LECTURE", result.slots[0].type.id)
        assertTrue(result.summary.contains("lectures"))
    }

    @Test
    fun `rejects wrong schema version`() {
        val result = importer.parse(VALID_JSON.replace("\"timetableSchemaVersion\": 1", "\"timetableSchemaVersion\": 9"))
        assertTrue(result.slots.isEmpty())
        assertTrue(result.errors.any { it.contains("timetableSchemaVersion") })
    }

    @Test
    fun `rejects invalid json`() {
        val result = importer.parse("not json at all {")
        assertTrue(result.slots.isEmpty())
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `skips invalid slot times`() {
        val bad = VALID_JSON.replace("\"start\": \"08:10\"", "\"start\": \"25:99\"")
        val result = importer.parse(bad)
        assertTrue(result.errors.any { it.contains("invalid time") })
    }

    @Test
    fun `skips unknown day`() {
        val bad = VALID_JSON.replace("\"day\": \"MONDAY\"", "\"day\": \"SUNDAY\"")
        val result = importer.parse(bad)
        assertTrue(result.slots.isEmpty())
        assertTrue(result.errors.any { it.contains("Unknown day") })
    }

    @Test
    fun `skips unknown type`() {
        val bad = VALID_JSON.replace("\"type\": \"LECTURE\"", "\"type\": \"XYZ\"")
        val result = importer.parse(bad)
        assertTrue(result.errors.any { it.contains("unknown type") })
    }

    companion object {
        private val VALID_JSON = """
            {
              "timetableSchemaVersion": 1,
              "source": "TE-B.pdf",
              "class": "TE-Div:B",
              "personalization": { "batch": "B1", "elective": "IP" },
              "days": [
                {
                  "day": "MONDAY",
                  "slots": [
                    { "slotNo": 1, "start": "08:10", "end": "09:05", "subject": "IP", "subjectFull": "Internet Programming", "room": "201", "faculty": "AAG", "type": "LECTURE" },
                    { "slotNo": 2, "start": "09:05", "end": "10:00", "subject": "AISC", "subjectFull": "AISC", "room": "203", "faculty": "MVC", "type": "LECTURE" },
                    { "slotNo": 3, "start": "10:20", "end": "11:15", "subject": "CN-Lab", "subjectFull": "CN Lab", "room": "314", "faculty": "SHM", "type": "LAB", "batch": "B1" },
                    { "slotNo": 4, "start": "11:15", "end": "12:10", "subject": null, "subjectFull": "FREE", "type": "FREE" },
                    { "slotNo": 5, "start": "12:50", "end": "13:45", "subject": "CN", "subjectFull": "CN", "room": "203", "faculty": "SHM", "type": "LECTURE" },
                    { "slotNo": 6, "start": "13:45", "end": "14:40", "subject": "MDM", "subjectFull": "MDM", "room": "203", "faculty": "SVF", "type": "LECTURE" },
                    { "slotNo": 7, "start": "14:40", "end": "15:35", "subject": "INDUSTRIAL TRAINING", "subjectFull": "Industrial Training", "type": "TRAINING" },
                    { "slotNo": 8, "start": "15:35", "end": "16:30", "subject": "FREE", "subjectFull": "FREE", "type": "FREE" }
                  ]
                }
              ]
            }
        """.trimIndent()
    }
}
