package com.example.data.database

import androidx.room.*
import com.example.data.model.WorkSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkSessionDao {
    @Query("SELECT * FROM work_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkSession>>

    @Query("SELECT * FROM work_sessions WHERE jobId = :jobId ORDER BY startTime DESC")
    fun getSessionsForJob(jobId: Int): Flow<List<WorkSession>>

    @Query("SELECT * FROM work_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(): WorkSession?

    @Query("SELECT * FROM work_sessions WHERE endTime IS NULL LIMIT 1")
    fun getActiveSessionFlow(): Flow<WorkSession?>

    @Query("SELECT * FROM work_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): WorkSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkSession): Long

    @Update
    suspend fun updateSession(session: WorkSession)

    @Delete
    suspend fun deleteSession(session: WorkSession)

    @Query("DELETE FROM work_sessions WHERE jobId = :jobId")
    suspend fun deleteSessionsForJob(jobId: Int)
}
