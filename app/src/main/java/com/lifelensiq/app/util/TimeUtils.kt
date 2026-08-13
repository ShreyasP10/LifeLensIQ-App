package com.lifelensiq.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimeUtils {

    val TIME = DateTimeFormatter.ofPattern("HH:mm")
    val DAY_NAMES = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")

    fun now(): Long = System.currentTimeMillis()

    fun todayName(): String = LocalDate.now().dayOfWeek.name.uppercase()

    fun parse(time: String): LocalTime = LocalTime.parse(time)

    /** Current or next slot for a given day list, based on now. */
    fun currentOrNext(slots: List<TimetableSlotComparable>, nowMs: Long): TimetableSlotComparable? {
        val now = LocalTime.now()
        val applicable = slots.filter { it.applicable && it.typeId !in setOf("FREE") }
        applicable.firstOrNull { slot ->
            val s = parse(slot.start)
            val e = parse(slot.end)
            !now.isBefore(s) && now.isBefore(e)
        }?.let { return it }
        return applicable.firstOrNull { !now.isAfter(parse(it.start)) }
    }

    fun todayEpochStart(): Long {
        val startOfDay = LocalDate.now().atStartOfDay()
        return startOfDay.toEpochSecond(java.time.ZoneId.systemDefault().rules.getOffset(startOfDay)) * 1000
    }
}

/** Small projection to keep TimeUtils decoupled from Room entities. */
data class TimetableSlotComparable(
    val day: String,
    val slotNo: Int,
    val start: String,
    val end: String,
    val subject: String,
    val subjectFull: String,
    val room: String,
    val typeId: String,
    val applicable: Boolean
) {
    fun toSlot(): com.lifelensiq.app.domain.model.TimetableSlot = com.lifelensiq.app.domain.model.TimetableSlot(
        day = day, slotNo = slotNo, start = start, end = end,
        subject = subject, subjectFull = subjectFull, room = room,
        faculty = "", type = com.lifelensiq.app.domain.model.SlotType.from(typeId) ?: com.lifelensiq.app.domain.model.SlotType.FREE,
        applicable = applicable
    )
}
