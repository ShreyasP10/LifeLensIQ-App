package com.lifeiq.app.domain.model

import com.lifeiq.app.data.local.TimetableEntity

/** Timetable slot types (mirrors 03_Data_Schema.md). */
enum class SlotType(val id: String) {
    LECTURE("LECTURE"),
    LAB("LAB"),
    BREAK("BREAK"),
    LUNCH("LUNCH"),
    TRAINING("TRAINING"),
    MENTORING("MENTORING"),
    OE("OE"),
    TE_HONOR("TE_HONOR"),
    FREE("FREE");

    companion object {
        fun from(id: String?): SlotType? = entries.firstOrNull { it.id == id }
        val ATTENDABLE = setOf(LECTURE, LAB)
    }
}

/** Immutable view of a timetable slot (maps to [TimetableEntity]). */
data class TimetableSlot(
    val day: String,
    val slotNo: Int,
    val start: String,
    val end: String,
    val subject: String,
    val subjectFull: String,
    val room: String,
    val faculty: String,
    val type: SlotType,
    val applicable: Boolean
) {
    fun toEntity(): TimetableEntity = TimetableEntity(
        day = day,
        slotNo = slotNo,
        start = start,
        end = end,
        subject = subject,
        subjectFull = subjectFull,
        room = room,
        faculty = faculty,
        type = type.id,
        applicable = applicable
    )

    companion object {
        fun from(entity: TimetableEntity) = TimetableSlot(
            day = entity.day,
            slotNo = entity.slotNo,
            start = entity.start,
            end = entity.end,
            subject = entity.subject,
            subjectFull = entity.subjectFull,
            room = entity.room,
            faculty = entity.faculty,
            type = SlotType.from(entity.type) ?: SlotType.FREE,
            applicable = entity.applicable
        )
    }
}

data class ExportFilter(
    val from: Long? = null,
    val to: Long? = null,
    val eventTypes: Set<String>? = null
)

data class SyncResult(
    val uploaded: Int,
    val failed: Boolean = false,
    val error: String? = null
)
