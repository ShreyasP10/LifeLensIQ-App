package com.lifelensiq.app.data.repository

import com.lifelensiq.app.data.local.AppDatabase
import com.lifelensiq.app.data.local.TimetableDao
import com.lifelensiq.app.data.local.TimetableEntity
import com.lifelensiq.app.data.remote.FirestoreEventSource
import com.lifelensiq.app.domain.model.TimetableSlot
import com.lifelensiq.app.domain.repository.AuthRepository
import com.lifelensiq.app.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimetableRepositoryImpl(
    private val db: AppDatabase,
    private val auth: AuthRepository,
    private val remote: FirestoreEventSource
) : TimetableRepository {

    private val dao: TimetableDao = db.timetableDao()

    override suspend fun saveAll(slots: List<TimetableSlot>, batch: String?) {
        dao.insertAll(slots.map { it.toEntity() })
        val uid = auth.userId ?: return
        runCatching {
            remote.uploadTimetable(uid, slots.map { it.toCloudMap() }, batch)
        }
    }

    override suspend fun getDay(day: String): List<TimetableSlot> =
        dao.getDay(day).map { TimetableSlot.from(it) }

    override fun observeAll(): Flow<List<TimetableSlot>> =
        dao.observeAll().map { list -> list.map { TimetableSlot.from(it) } }

    override fun observeDay(day: String): Flow<List<TimetableSlot>> =
        dao.observeDay(day).map { list -> list.map { TimetableSlot.from(it) } }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}

private fun TimetableSlot.toCloudMap(): Map<String, Any?> = mapOf(
    "day" to day,
    "slotNo" to slotNo,
    "startTime" to start,
    "endTime" to end,
    "subject" to subject,
    "subjectFull" to subjectFull,
    "room" to room,
    "faculty" to faculty,
    "type" to type.id,
    "applicable" to applicable
)
