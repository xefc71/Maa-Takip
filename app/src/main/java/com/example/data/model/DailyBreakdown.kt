package com.example.data.model

data class DailyBreakdown(
    val dateMillis: Long, // Start of the day (midnight) in epoch millis
    val dateString: String, // e.g. "29 Haziran Pazartesi"
    val netHours: Double = 0.0,
    val normalHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val nightHours: Double = 0.0,
    val weekendHours: Double = 0.0,
    val holidayHours: Double = 0.0,
    val totalBreakMinutes: Int = 0,
    val earnings: Double = 0.0,
    val isRestDay: Boolean = false,
    val isHoliday: Boolean = false,
    val note: String? = null
)
