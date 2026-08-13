package com.lifelensiq.app.timetable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DTOs matching the timetable import JSON contract (see assets/docs/03_Data_Schema.md §5). */
@Serializable
data class TimetableImportDto(
    val timetableSchemaVersion: Int = 1,
    val source: String = "",
    @SerialName("class") val className: String = "",
    val academicYear: String = "",
    val personalization: PersonalizationDto? = null,
    val days: List<DayDto> = emptyList()
)

@Serializable
data class PersonalizationDto(
    val batch: String = "",
    val elective: String = "",
    val teHonorTaken: Boolean = false,
    val oeTaken: Boolean = false
)

@Serializable
data class DayDto(
    val day: String,
    val slots: List<SlotDto>
)

@Serializable
data class SlotDto(
    val slotNo: Int,
    val start: String,
    val end: String,
    val subject: String? = null,
    val subjectFull: String = "",
    val room: String = "",
    val faculty: String = "",
    val type: String,
    val applicable: Boolean = true,
    val batch: String? = null
)
