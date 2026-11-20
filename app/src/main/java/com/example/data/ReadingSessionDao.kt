package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions ORDER BY date ASC, id ASC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY date ASC, id ASC")
    suspend fun getAllSessionsList(): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions ORDER BY id DESC LIMIT 1")
    suspend fun getLastSession(): ReadingSessionEntity?
}
