package com.lifeiq.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [Index("synced"), Index("timestamp"), Index("eventType")]
)
data class EventEntity(
    @PrimaryKey val eventId: String,
    val userId: String,
    val deviceId: String,
    val eventType: String,
    val payloadJson: String,
    val timestamp: Long,
    val schemaVersion: Int = 1,
    val synced: Boolean = false
)
