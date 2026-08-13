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
            val uploaded = remote.uploadBatch(events)
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
        db.timetableDao().deleteAll()
        syncLogDao.deleteAll()
    }

    override suspend fun deleteAllCloud() {
        val uid = auth.userId ?: return
        remote.deleteAllUserData(uid)
    }
}
