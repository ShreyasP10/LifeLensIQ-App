package com.lifeiq.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<TimetableEntity>)

    @Query("SELECT * FROM timetable WHERE day = :day ORDER BY slotNo ASC")
    suspend fun getDay(day: String): List<TimetableEntity>

    @Query("SELECT * FROM timetable WHERE day = :day ORDER BY slotNo ASC")
    fun observeDay(day: String): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable ORDER BY slotNo ASC")
    fun observeAll(): Flow<List<TimetableEntity>>

    @Query("DELETE FROM timetable")
    suspend fun deleteAll(): Int
}
