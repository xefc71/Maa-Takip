package com.example.data.repository

import com.example.data.database.JobDao
import com.example.data.database.WorkSessionDao
import com.example.data.model.Job
import com.example.data.model.WorkSession
import kotlinx.coroutines.flow.Flow

class WorkHoursRepository(
    private val jobDao: JobDao,
    private val workSessionDao: WorkSessionDao
) {
    val allJobs: Flow<List<Job>> = jobDao.getAllJobs()
    val activeJobFlow: Flow<Job?> = jobDao.getActiveJobFlow()
    val allSessions: Flow<List<WorkSession>> = workSessionDao.getAllSessions()
    val activeSessionFlow: Flow<WorkSession?> = workSessionDao.getActiveSessionFlow()

    fun getSessionsForJob(jobId: Int): Flow<List<WorkSession>> {
        return workSessionDao.getSessionsForJob(jobId)
    }

    suspend fun getActiveJob(): Job? = jobDao.getActiveJob()

    suspend fun getActiveSession(): WorkSession? = workSessionDao.getActiveSession()

    suspend fun insertJob(job: Job): Long = jobDao.insertJob(job)

    suspend fun updateJob(job: Job) = jobDao.updateJob(job)

    suspend fun deleteJob(job: Job) {
        workSessionDao.deleteSessionsForJob(job.id)
        jobDao.deleteJob(job)
    }

    suspend fun setActiveJob(jobId: Int) = jobDao.setActiveJob(jobId)

    suspend fun insertSession(session: WorkSession): Long = workSessionDao.insertSession(session)

    suspend fun updateSession(session: WorkSession) = workSessionDao.updateSession(session)

    suspend fun deleteSession(session: WorkSession) = workSessionDao.deleteSession(session)
}
