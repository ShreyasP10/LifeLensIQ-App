package com.lifeiq.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Query("SELECT * FROM events WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsynced(limit: Int): List<EventEntity>

    @Query("UPDATE events SET synced = 1 WHERE eventId IN (:ids)")
    suspend fun markSynced(ids: List<String>): Int

    @Query("SELECT COUNT(*) FROM events WHERE synced = 0")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM events WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    fun observeBetween(from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    suspend fun getBetween(from: Long, to: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE eventType = :type AND timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    suspend fun getByTypeBetween(type: String, from: Long, to: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE synced = 1 AND timestamp < :cutoff")
    suspend fun getSyncedBefore(cutoff: Long): List<EventEntity>

    @Query("DELETE FROM events WHERE eventId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int

    @Query("DELETE FROM events")
    suspend fun deleteAll(): Int
}
