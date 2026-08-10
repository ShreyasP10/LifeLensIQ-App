package com.lifeiq.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_log")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncedAt: Long,
    val batchSize: Int,
    val success: Boolean,
    val error: String? = null
)
