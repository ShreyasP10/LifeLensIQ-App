package com.lifelensiq.app.domain.repository

import com.lifelensiq.app.domain.model.TimetableSlot
import kotlinx.coroutines.flow.Flow

interface TimetableRepository {
    suspend fun saveAll(slots: List<TimetableSlot>, batch: String? = null)
    suspend fun getDay(day: String): List<TimetableSlot>
    fun observeAll(): Flow<List<TimetableSlot>>
    fun observeDay(day: String): Flow<List<TimetableSlot>>
    suspend fun deleteAll()
}
