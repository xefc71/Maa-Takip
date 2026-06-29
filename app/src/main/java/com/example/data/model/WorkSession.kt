package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_sessions")
data class WorkSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int,
    val startTime: Long, // epoch milliseconds
    val endTime: Long?, // epoch milliseconds, null if currently active
    val manualBreakDurationMinutes: Int = 0,
    val note: String? = null,
    val isManualEntry: Boolean = false,
    val isHoliday: Boolean = false,
    val isWeekend: Boolean = false,
    val customBreaksJson: String = "[]"
)
