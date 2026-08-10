package com.lifeiq.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SyncLogDao {

    @Insert
    suspend fun insert(log: SyncLogEntity)

    @Query("SELECT * FROM sync_log ORDER BY id DESC LIMIT 20")
    suspend fun getRecent(): List<SyncLogEntity>

    @Query("DELETE FROM sync_log")
    suspend fun deleteAll(): Int
}
