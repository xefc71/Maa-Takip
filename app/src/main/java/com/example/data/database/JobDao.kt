package com.example.data.database

import androidx.room.*
import com.example.data.model.Job
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY id ASC")
    fun getAllJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE isActive = 1 LIMIT 1")
    fun getActiveJobFlow(): Flow<Job?>

    @Query("SELECT * FROM jobs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveJob(): Job?

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getJobById(id: Int): Job?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: Job): Long

    @Update
    suspend fun updateJob(job: Job)

    @Delete
    suspend fun deleteJob(job: Job)

    @Query("UPDATE jobs SET isActive = 0")
    suspend fun deactivateAllJobs()

    @Transaction
    suspend fun setActiveJob(jobId: Int) {
        deactivateAllJobs()
        val job = getJobById(jobId)
        if (job != null) {
            updateJob(job.copy(isActive = true))
        }
    }
}
