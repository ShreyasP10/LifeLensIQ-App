package com.lifeiq.app.data.repository

import com.lifeiq.app.data.local.AppDatabase
import com.lifeiq.app.data.local.TimetableDao
import com.lifeiq.app.data.local.TimetableEntity
import com.lifeiq.app.data.remote.FirestoreEventSource
import com.lifeiq.app.domain.model.TimetableSlot
import com.lifeiq.app.domain.repository.AuthRepository
import com.lifeiq.app.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimetableRepositoryImpl(
    private val db: AppDatabase,
    private val auth: AuthRepository,
    private val remote: FirestoreEventSource
) : TimetableRepository {

    private val dao: TimetableDao = db.timetableDao()

    override suspend fun saveAll(slots: List<TimetableSlot>) {
        dao.insertAll(slots.map { it.toEntity() })
        val uid = auth.userId ?: return
        runCatching {
            slots.groupBy { it.day }.forEach { (day, daySlots) ->
                remote.uploadTimetable(uid, day, daySlots.map { it.toCloudMap() })
            }
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
    "slotNo" to slotNo,
    "start" to start,
    "end" to end,
    "subject" to subject,
    "subjectFull" to subjectFull,
    "room" to room,
    "faculty" to faculty,
    "type" to type.id,
    "applicable" to applicable
)
