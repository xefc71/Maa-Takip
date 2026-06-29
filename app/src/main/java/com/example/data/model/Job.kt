package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val employmentType: String, // "MAASLI", "YEVMIYELI", "SAAT_UCRETLI"
    val monthlySalary: Double = 0.0,
    val dailyWage: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val standardStartTime: String = "08:00",
    val standardEndTime: String = "18:00",
    val weeklyWorkDays: String = "Pazartesi,Salı,Çarşamba,Perşembe,Cuma,Cumartesi", // Comma-separated
    val weeklyRestDays: String = "Pazar", // Comma-separated
    val overtimeMultiplier: Double = 1.5,
    val nightShiftMultiplier: Double = 1.25,
    val weekendMultiplier: Double = 1.5,
    val holidayMultiplier: Double = 2.0,
    val lunchBreakStart: String = "12:00",
    val lunchBreakEnd: String = "13:00",
    val coffeeBreakStart: String = "10:00",
    val coffeeBreakEnd: String = "10:15",
    val isBreakAutoDeduct: Boolean = true,
    val isLunchBreakEnabled: Boolean = true,
    val isCoffeeBreakEnabled: Boolean = true,
    val extraBreakMinutes: Int = 0,
    val isActive: Boolean = false
)
