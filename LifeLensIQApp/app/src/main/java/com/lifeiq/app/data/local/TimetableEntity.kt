package com.lifeiq.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable",
    indices = [Index(value = ["day", "slotNo"], unique = true)]
)
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: String,
    val slotNo: Int,
    val start: String,
    val end: String,
    val subject: String,
    val subjectFull: String,
    val room: String,
    val faculty: String,
    val type: String,
    val applicable: Boolean
)
