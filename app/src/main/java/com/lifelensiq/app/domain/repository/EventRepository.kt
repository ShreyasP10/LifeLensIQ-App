package com.lifelensiq.app.domain.repository

import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.data.local.SyncLogEntity
import com.lifelensiq.app.domain.model.SyncResult
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for events: local Room buffer (primary) + Firestore (cloud copy).
 * All writes are write-through to Room; cloud sync happens via [syncBatch].
 */
interface EventRepository {
    /** Persist an event locally and return its eventId. */
    suspend fun emit(eventType: String, payload: Map<String, Any?>): String

    suspend fun getUnsynced(limit: Int): List<EventEntity>

    /** Upload a batch to Firestore; on success marks them synced locally. */
    suspend fun syncBatch(events: List<EventEntity>): SyncResult

    fun observePendingCount(): Flow<Int>
    fun observeEvents(from: Long, to: Long): Flow<List<EventEntity>>
    suspend fun eventsBetween(from: Long, to: Long): List<EventEntity>
    suspend fun getSyncLog(): List<SyncLogEntity>
    suspend fun deleteAllLocal()
    suspend fun deleteAllCloud()
}
