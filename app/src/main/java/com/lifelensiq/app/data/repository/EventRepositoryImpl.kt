package com.lifelensiq.app.data.repository

import com.lifelensiq.app.data.local.AppDatabase
import com.lifelensiq.app.data.local.EventDao
import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.data.local.SyncLogDao
import com.lifelensiq.app.data.local.SyncLogEntity
import com.lifelensiq.app.data.remote.FirestoreEventSource
import com.lifelensiq.app.domain.repository.AuthRepository
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.domain.model.SyncResult
import com.lifelensiq.app.util.DeviceIdProvider
import com.lifelensiq.app.util.JsonUtil
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class EventRepositoryImpl(
    private val db: AppDatabase,
    private val auth: AuthRepository,
    private val remote: FirestoreEventSource,
    private val deviceId: String
) : EventRepository {

    private val eventDao: EventDao = db.eventDao()
    private val syncLogDao: SyncLogDao = db.syncLogDao()

    override suspend fun emit(eventType: String, payload: Map<String, Any?>): String {
        val event = EventEntity(
            eventId = UUID.randomUUID().toString(),
            userId = auth.userId ?: "anonymous",
            deviceId = deviceId,
            eventType = eventType,
            payloadJson = JsonUtil.encodePayload(payload),
            timestamp = System.currentTimeMillis(),
            synced = false
        )
        eventDao.insert(event)
        return event.eventId
    }

    override suspend fun getUnsynced(limit: Int): List<EventEntity> = eventDao.getUnsynced(limit)

    override suspend fun syncBatch(events: List<EventEntity>): SyncResult {
        if (events.isEmpty()) return SyncResult(0)
        return try {
            val userId = auth.userId ?: return SyncResult(0, failed = true, error = "Not logged in")
            val uploaded = remote.uploadBatch(userId, events)
            eventDao.markSynced(events.map { it.eventId })
            syncLogDao.insert(SyncLogEntity(syncedAt = System.currentTimeMillis(), batchSize = uploaded, success = true))
            SyncResult(uploaded)
        } catch (t: Throwable) {
            syncLogDao.insert(SyncLogEntity(syncedAt = System.currentTimeMillis(), batchSize = 0, success = false, error = t.message))
            SyncResult(0, failed = true, error = t.message)
        }
    }

    override fun observePendingCount(): Flow<Int> = eventDao.observePendingCount()

    override fun observeEvents(from: Long, to: Long): Flow<List<EventEntity>> =
        eventDao.observeBetween(from, to)

    override suspend fun eventsBetween(from: Long, to: Long): List<EventEntity> =
        eventDao.getBetween(from, to)

    override suspend fun getSyncLog(): List<SyncLogEntity> = syncLogDao.getRecent()

    override suspend fun deleteAllLocal() {
        eventDao.deleteAll()
        syncLogDao.deleteAll()
    }

    override suspend fun deleteAllCloud() {
        val uid = auth.userId ?: return
        remote.deleteAllUserData(uid)
    }

    override suspend fun downloadCloud(): Int {
        val uid = auth.userId ?: return 0
        return try {
            val docs = remote.fetchAllEvents(uid)
            val entities = docs.mapNotNull { it.toEventEntity() }
            if (entities.isNotEmpty()) eventDao.insertAll(entities)
            entities.size
        } catch (t: Throwable) {
            // Uploads already succeeded; a failed pull must not fail the run —
            // but log it so Settings shows why website data is missing.
            syncLogDao.insert(
                SyncLogEntity(
                    syncedAt = System.currentTimeMillis(),
                    batchSize = 0,
                    success = false,
                    error = "pull: ${t.message}"
                )
            )
            0
        }
    }

    override suspend fun prune(retentionMs: Long) {
        val cutoff = System.currentTimeMillis() - retentionMs
        eventDao.deleteSyncedBefore(cutoff)
        syncLogDao.deleteBefore(cutoff)
    }
}

/**
 * Maps a cloud doc (web dashboard envelope) back into a local event.
 * Accepts both the web format (`id`/`ts`/`metadata`) and the app format
 * (`eventId`/`timestamp`/`payload`). Deduplication is automatic — Room
 * REPLACEs by primary key `eventId`.
 */
private fun Map<String, Any?>.toEventEntity(): EventEntity? {
    val eventId = (this["eventId"] as? String) ?: (this["id"] as? String) ?: return null
    val timestamp = (this["timestamp"] as? Number)?.toLong()
        ?: (this["ts"] as? Number)?.toLong()
        ?: (this["timestamp"] as? com.google.firebase.Timestamp)?.let { it.toDate().time }
        ?: (this["ts"] as? com.google.firebase.Timestamp)?.let { it.toDate().time }
        ?: return null
    val eventType = (this["eventType"] as? String) ?: return null
    val payload = ((this["metadata"] as? Map<*, *>) ?: (this["payload"] as? Map<*, *>) ?: emptyMap<String, Any?>())
        .mapKeys { it.key.toString() }
        .toMutableMap()
    // The web dashboard writes durationSeconds at the top level; the app
    // reads durationMs from the payload — synthesize it when missing so
    // website-written sessions show real durations everywhere.
    if (payload["durationMs"] == null) {
        val durationSeconds = (this["durationSeconds"] as? Number)?.toLong()
        if (durationSeconds != null) payload["durationMs"] = durationSeconds * 1000
    }
    if (payload["startedAt"] == null && this["ts"] != null) {
        payload["startedAt"] = timestamp
    }
    return EventEntity(
        eventId = eventId,
        userId = (this["userId"] as? String) ?: "anonymous",
        deviceId = (this["deviceId"] as? String) ?: (this["device"] as? String) ?: "",
        eventType = eventType,
        payloadJson = JsonUtil.encodePayload(payload),
        timestamp = timestamp,
        synced = true
    )
}
