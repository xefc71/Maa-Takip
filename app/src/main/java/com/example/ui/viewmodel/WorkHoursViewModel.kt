package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.DailyBreakdown
import com.example.data.model.Job
import com.example.data.model.WorkSession
import com.example.data.repository.WorkHoursRepository
import com.example.util.TimeEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class WorkHoursViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WorkHoursRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WorkHoursRepository(db.jobDao(), db.workSessionDao())
    }

    val allJobs: StateFlow<List<Job>> = repository.allJobs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeJob: StateFlow<Job?> = repository.activeJobFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSession: StateFlow<WorkSession?> = repository.activeSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentSelectedDate = MutableStateFlow(System.currentTimeMillis())
    val currentSelectedDate: StateFlow<Long> = _currentSelectedDate.asStateFlow()

    val allSessions: StateFlow<List<WorkSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveTicker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val activeJobMonthlyBreakdowns: StateFlow<List<DailyBreakdown>> = combine(
        activeJob,
        allSessions,
        _currentSelectedDate
    ) { job, sessions, selectedDate ->
        if (job == null) return@combine emptyList<DailyBreakdown>()

        val calendar = Calendar.getInstance(Locale("tr", "TR"))
        calendar.timeInMillis = selectedDate
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = TimeEngine.getStartOfDay(calendar.timeInMillis)
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val monthEnd = TimeEngine.getStartOfDay(calendar.timeInMillis) + 24 * 60 * 60 * 1000L - 1

        val jobSessions = sessions.filter { 
            it.jobId == job.id && 
            it.startTime >= monthStart - 2 * 24 * 60 * 60 * 1000L && 
            (it.endTime ?: System.currentTimeMillis()) <= monthEnd + 2 * 24 * 60 * 60 * 1000L 
        }
        
        val jobStartDate = sessions.filter { it.jobId == job.id }.minOfOrNull { it.startTime }?.let { TimeEngine.getStartOfDay(it) }

        TimeEngine.calculateDailyBreakdowns(job, jobSessions, monthStart, monthEnd, jobStartDate).values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allJobsCombinedMonthlyBreakdowns: StateFlow<Map<Int, List<DailyBreakdown>>> = combine(
        allJobs,
        allSessions,
        _currentSelectedDate
    ) { jobs, sessions, selectedDate ->
        val calendar = Calendar.getInstance(Locale("tr", "TR"))
        calendar.timeInMillis = selectedDate
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = TimeEngine.getStartOfDay(calendar.timeInMillis)
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val monthEnd = TimeEngine.getStartOfDay(calendar.timeInMillis) + 24 * 60 * 60 * 1000L - 1

        jobs.associate { job ->
            val jobSessions = sessions.filter { 
                it.jobId == job.id && 
                it.startTime >= monthStart - 2 * 24 * 60 * 60 * 1000L && 
                (it.endTime ?: System.currentTimeMillis()) <= monthEnd + 2 * 24 * 60 * 60 * 1000L 
            }
            val jobStartDate = sessions.filter { it.jobId == job.id }.minOfOrNull { it.startTime }?.let { TimeEngine.getStartOfDay(it) }
            job.id to TimeEngine.calculateDailyBreakdowns(job, jobSessions, monthStart, monthEnd, jobStartDate).values.toList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectJob(jobId: Int) {
        viewModelScope.launch {
            repository.setActiveJob(jobId)
        }
    }

    fun startWork(jobId: Int) {
        viewModelScope.launch {
            val currentActive = repository.getActiveSession()
            if (currentActive == null) {
                repository.insertSession(
                    WorkSession(
                        jobId = jobId,
                        startTime = System.currentTimeMillis(),
                        endTime = null
                    )
                )
            }
        }
    }

    fun endWork(note: String? = null, manualBreakMinutes: Int = 0, isHoliday: Boolean = false, isWeekend: Boolean = false) {
        viewModelScope.launch {
            val currentActive = repository.getActiveSession()
            if (currentActive != null) {
                repository.updateSession(
                    currentActive.copy(
                        endTime = System.currentTimeMillis(),
                        note = note,
                        manualBreakDurationMinutes = manualBreakMinutes,
                        isHoliday = isHoliday,
                        isWeekend = isWeekend
                    )
                )
            }
        }
    }

    fun addJob(job: Job) {
        viewModelScope.launch {
            val isFirst = allJobs.value.isEmpty()
            val insertedId = repository.insertJob(job.copy(isActive = isFirst))
            if (isFirst) {
                repository.setActiveJob(insertedId.toInt())
            }
        }
    }

    fun updateJob(job: Job) {
        viewModelScope.launch {
            repository.updateJob(job)
        }
    }

    fun deleteJob(job: Job) {
        viewModelScope.launch {
            repository.deleteJob(job)
            if (job.isActive) {
                val remaining = allJobs.value.filter { it.id != job.id }
                if (remaining.isNotEmpty()) {
                    repository.setActiveJob(remaining.first().id)
                }
            }
        }
    }

    fun addManualSession(session: WorkSession) {
        viewModelScope.launch {
            repository.insertSession(session)
        }
    }

    fun updateManualSession(session: WorkSession) {
        viewModelScope.launch {
            repository.updateSession(session)
        }
    }

    fun deleteSession(session: WorkSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance(Locale("tr", "TR"))
        cal.timeInMillis = _currentSelectedDate.value
        cal.add(Calendar.MONTH, offset)
        _currentSelectedDate.value = cal.timeInMillis
    }
}
