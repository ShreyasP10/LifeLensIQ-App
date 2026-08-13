package com.lifelensiq.app.timetable

import com.lifelensiq.app.domain.model.SlotType
import com.lifelensiq.app.domain.model.TimetableSlot
import com.lifelensiq.app.util.JsonUtil

/**
 * Parses and validates timetable_personalized.json (output of fetch_timetable.py).
 * Pure JVM — no Android dependencies.
 */
class TimetableImporter {

    data class ImportResult(
        val slots: List<TimetableSlot>,
        val summary: String,
        val errors: List<String> = emptyList(),
        val batch: String? = null
    )

    fun parse(rawJson: String): ImportResult {
        val errors = mutableListOf<String>()
        val dto = runCatching { JsonUtil.json.decodeFromString<TimetableImportDto>(rawJson) }
            .getOrElse { t ->
                return ImportResult(emptyList(), "Import failed", listOf(t.message ?: "Invalid JSON"))
            }

        if (dto.timetableSchemaVersion != SUPPORTED_VERSION) {
            errors += "Unsupported timetableSchemaVersion=${dto.timetableSchemaVersion} (expected $SUPPORTED_VERSION)"
            return ImportResult(emptyList(), "Import rejected", errors)
        }

        val slots = mutableListOf<TimetableSlot>()
        val seen = mutableSetOf<Pair<String, Int>>()

        for (dayDto in dto.days) {
            val day = dayDto.day.trim().uppercase()
            if (day !in VALID_DAYS) {
                errors += "Unknown day '${dayDto.day}' (skipped)"
                continue
            }
            for (slot in dayDto.slots) {
                val key = day to slot.slotNo
                if (!seen.add(key)) {
                    errors += "Duplicate slot $day #${slot.slotNo} (skipped)"
                    continue
                }
                if (slot.slotNo !in 1..8) {
                    errors += "$day #${slot.slotNo}: slotNo must be 1..8 (skipped)"
                    continue
                }
                if (!TIME_REGEX.matches(slot.start) || !TIME_REGEX.matches(slot.end)) {
                    errors += "$day #${slot.slotNo}: invalid time (skipped)"
                    continue
                }
                if (slot.start >= slot.end) {
                    errors += "$day #${slot.slotNo}: start must be before end (skipped)"
                    continue
                }
                val type = SlotType.from(slot.type.uppercase())
                if (type == null) {
                    errors += "$day #${slot.slotNo}: unknown type '${slot.type}' (skipped)"
                    continue
                }
                slots += TimetableSlot(
                    day = day,
                    slotNo = slot.slotNo,
                    start = slot.start,
                    end = slot.end,
                    subject = slot.subject ?: "FREE",
                    subjectFull = slot.subjectFull.ifBlank { slot.subject ?: "FREE PERIOD" },
                    room = slot.room,
                    faculty = slot.faculty,
                    type = type,
                    applicable = slot.applicable
                )
            }
        }

        val lectureCount = slots.count { it.type == SlotType.LECTURE }
        val labCount = slots.count { it.type == SlotType.LAB }
        val freeCount = slots.count { it.type == SlotType.FREE }
        val otherCount = slots.size - lectureCount - labCount - freeCount
        val summary = "$lectureCount lectures, $labCount labs, $freeCount free, $otherCount other (training/mentoring/break) — " +
            "personalized for ${dto.personalization?.batch ?: "?"} / ${dto.personalization?.elective ?: "?"}"

        return ImportResult(slots, summary, errors, dto.personalization?.batch)
    }

    companion object {
        const val SUPPORTED_VERSION = 1
        private val VALID_DAYS = setOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        private val TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
    }
}
