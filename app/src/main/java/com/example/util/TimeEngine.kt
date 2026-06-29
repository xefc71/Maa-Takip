package com.example.util

import com.example.data.model.Job
import com.example.data.model.WorkSession
import com.example.data.model.DailyBreakdown
import java.text.SimpleDateFormat
import java.util.*

object TimeEngine {
    private val turkishLocale = Locale("tr", "TR")

    fun getStartOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance(turkishLocale)
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun formatDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("d MMMM EEEE", turkishLocale)
        return sdf.format(Date(timeMillis))
    }

    fun formatMonthYear(timeMillis: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", turkishLocale)
        return sdf.format(Date(timeMillis))
    }

    fun formatTime(timeMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", turkishLocale)
        return sdf.format(Date(timeMillis))
    }

    fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }

    // Splits a work session if it spans across midnight.
    fun splitSessionByDay(session: WorkSession): List<Pair<Long, Long>> {
        val start = session.startTime
        val end = session.endTime ?: System.currentTimeMillis()
        if (start >= end) return emptyList()

        val list = mutableListOf<Pair<Long, Long>>()
        var currentStart = start

        while (true) {
            val startOfDay = getStartOfDay(currentStart)
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1 // 23:59:59.999

            if (end <= endOfDay) {
                list.add(Pair(currentStart, end))
                break
            } else {
                list.add(Pair(currentStart, endOfDay + 1)) // end segment exactly at next midnight
                currentStart = endOfDay + 1
            }
        }
        return list
    }

    // Calculates the overlap (in milliseconds) of an interval with a target time window of a specific day
    fun getOverlapWithTimeWindow(
        startMillis: Long,
        endMillis: Long,
        windowStartStr: String,
        windowEndStr: String,
        dayStartMillis: Long
    ): Long {
        val windowStartMinutes = parseTimeToMinutes(windowStartStr)
        val windowEndMinutes = parseTimeToMinutes(windowEndStr)

        val windowStartMillis = dayStartMillis + windowStartMinutes * 60L * 1000L
        var windowEndMillis = dayStartMillis + windowEndMinutes * 60L * 1000L

        // If end of window is before or equal to start, it's overnight window
        if (windowEndMinutes <= windowStartMinutes) {
            windowEndMillis += 24 * 60 * 60 * 1000L
        }

        val overlapStart = maxOf(startMillis, windowStartMillis)
        val overlapEnd = minOf(endMillis, windowEndMillis)

        return if (overlapStart < overlapEnd) overlapEnd - overlapStart else 0L
    }

    fun calculateDailyBreakdowns(
        job: Job,
        sessions: List<WorkSession>,
        monthStartMillis: Long,
        monthEndMillis: Long,
        jobStartDate: Long? = null
    ): Map<Long, DailyBreakdown> {
        val map = TreeMap<Long, DailyBreakdown>()

        // 1. Initialize all days of the month
        val cal = Calendar.getInstance(turkishLocale)
        cal.timeInMillis = monthStartMillis
        val endCal = Calendar.getInstance(turkishLocale)
        endCal.timeInMillis = monthEndMillis

        while (cal.timeInMillis <= endCal.timeInMillis) {
            val dayStart = getStartOfDay(cal.timeInMillis)
            val dayOfWeekStr = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, turkishLocale) ?: ""
            
            // Check if rest day
            val isRestDay = job.weeklyRestDays.split(",").any { it.trim().equals(dayOfWeekStr, ignoreCase = true) }

            map[dayStart] = DailyBreakdown(
                dateMillis = dayStart,
                dateString = formatDate(dayStart),
                isRestDay = isRestDay,
                isHoliday = false
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 2. Accumulate hours from sessions
        for (session in sessions) {
            val segments = splitSessionByDay(session)
            for (segment in segments) {
                val segStart = segment.first
                val segEnd = segment.second
                val dayStart = getStartOfDay(segStart)

                val dayRecord = map[dayStart] ?: continue

                val isHoliday = session.isHoliday
                val isWeekend = session.isWeekend || dayRecord.isRestDay

                // Calculate breaks
                var lunchBreakMinutes = 0
                var coffeeBreakMinutes = 0
                if (job.isBreakAutoDeduct) {
                    if (job.isLunchBreakEnabled) {
                        lunchBreakMinutes = (getOverlapWithTimeWindow(
                            segStart, segEnd, job.lunchBreakStart, job.lunchBreakEnd, dayStart
                        ) / (60 * 1000)).toInt()
                    }

                    if (job.isCoffeeBreakEnabled) {
                        coffeeBreakMinutes = (getOverlapWithTimeWindow(
                            segStart, segEnd, job.coffeeBreakStart, job.coffeeBreakEnd, dayStart
                        ) / (60 * 1000)).toInt()
                    }
                }

                val customBreaks = com.example.data.model.CustomBreak.fromJsonArrayString(session.customBreaksJson)
                val customBreakMin = customBreaks.sumOf { it.durationMinutes }

                val autoBreakMin = lunchBreakMinutes + coffeeBreakMinutes + if (job.isBreakAutoDeduct) job.extraBreakMinutes else 0
                val totalBreakMin = autoBreakMin + session.manualBreakDurationMinutes + customBreakMin

                val totalDurationMin = ((segEnd - segStart) / (60 * 1000)).toInt()
                val netDurationMin = maxOf(0, totalDurationMin - totalBreakMin)
                val netHours = netDurationMin / 60.0

                // Overtime: standard end time
                val standardEndMin = parseTimeToMinutes(job.standardEndTime)
                val standardEndMillis = dayStart + standardEndMin * 60L * 1000L

                var normalSegEnd = minOf(segEnd, standardEndMillis)
                var normalDurationMin = ((normalSegEnd - segStart) / (60 * 1000)).toInt()
                normalDurationMin = maxOf(0, normalDurationMin - totalBreakMin)
                val normalHoursRaw = normalDurationMin / 60.0

                val overtimeDurationMin = maxOf(0, netDurationMin - normalDurationMin)
                val overtimeHoursRaw = overtimeDurationMin / 60.0

                // Night shift window 22:00 to 06:00
                val nightHoursRaw = (getOverlapWithTimeWindow(
                    segStart, segEnd, "22:00", "06:00", dayStart
                ) / (3600.0 * 1000.0))

                var accumulatedNormal = dayRecord.normalHours
                var accumulatedOvertime = dayRecord.overtimeHours
                var accumulatedNight = dayRecord.nightHours
                var accumulatedWeekend = dayRecord.weekendHours
                var accumulatedHoliday = dayRecord.holidayHours

                if (isHoliday) {
                    accumulatedHoliday += netHours
                } else if (isWeekend) {
                    accumulatedWeekend += netHours
                } else {
                    accumulatedNormal += normalHoursRaw
                    accumulatedOvertime += overtimeHoursRaw
                }
                accumulatedNight += nightHoursRaw

                map[dayStart] = dayRecord.copy(
                    netHours = dayRecord.netHours + netHours,
                    normalHours = accumulatedNormal,
                    overtimeHours = accumulatedOvertime,
                    nightHours = accumulatedNight,
                    weekendHours = accumulatedWeekend,
                    holidayHours = accumulatedHoliday,
                    totalBreakMinutes = dayRecord.totalBreakMinutes + totalBreakMin,
                    isHoliday = isHoliday || dayRecord.isHoliday,
                    note = if (session.note != null) {
                        if (dayRecord.note != null) "${dayRecord.note}; ${session.note}" else session.note
                    } else dayRecord.note
                )
            }
        }

        // 3. Compute earnings per day
        val todayStart = getStartOfDay(System.currentTimeMillis())

        for ((dayStart, record) in map) {
            val computedHourlyRate = when (job.employmentType) {
                "MAASLI" -> {
                    val stdStartMin = parseTimeToMinutes(job.standardStartTime)
                    val stdEndMin = parseTimeToMinutes(job.standardEndTime)
                    val stdBreakMin = parseTimeToMinutes(job.lunchBreakEnd) - parseTimeToMinutes(job.lunchBreakStart) +
                                      (parseTimeToMinutes(job.coffeeBreakEnd) - parseTimeToMinutes(job.coffeeBreakStart))
                    val dailyStdWorkHours = maxOf(1.0, (stdEndMin - stdStartMin - stdBreakMin) / 60.0)
                    job.monthlySalary / (30.0 * dailyStdWorkHours)
                }
                "YEVMİYELİ" -> {
                    val stdStartMin = parseTimeToMinutes(job.standardStartTime)
                    val stdEndMin = parseTimeToMinutes(job.standardEndTime)
                    val stdBreakMin = parseTimeToMinutes(job.lunchBreakEnd) - parseTimeToMinutes(job.lunchBreakStart) +
                                      (parseTimeToMinutes(job.coffeeBreakEnd) - parseTimeToMinutes(job.coffeeBreakStart))
                    val dailyStdWorkHours = maxOf(1.0, (stdEndMin - stdStartMin - stdBreakMin) / 60.0)
                    job.dailyWage / dailyStdWorkHours
                }
                else -> {
                    job.hourlyRate
                }
            }

            var dailyEarnings = 0.0

            if (jobStartDate != null && dayStart < jobStartDate) {
                dailyEarnings = 0.0
            } else {
                val isFutureDay = dayStart > todayStart

                if (job.employmentType == "MAASLI") {
                    val stdStartMin = parseTimeToMinutes(job.standardStartTime)
                    val stdEndMin = parseTimeToMinutes(job.standardEndTime)
                    val stdBreakMin = parseTimeToMinutes(job.lunchBreakEnd) - parseTimeToMinutes(job.lunchBreakStart) +
                                      (parseTimeToMinutes(job.coffeeBreakEnd) - parseTimeToMinutes(job.coffeeBreakStart))
                    val dailyStdWorkHours = maxOf(1.0, (stdEndMin - stdStartMin - stdBreakMin) / 60.0)
                    val baseDailySalary = job.monthlySalary / 30.0

                    val workedNormalEarnings = minOf(record.normalHours, dailyStdWorkHours) * computedHourlyRate
                    val extraNormalEarnings = maxOf(0.0, record.normalHours - dailyStdWorkHours) * computedHourlyRate * job.overtimeMultiplier
                    val overtimeEarnings = record.overtimeHours * computedHourlyRate * job.overtimeMultiplier
                    val nightEarnings = record.nightHours * computedHourlyRate * (job.nightShiftMultiplier - 1.0)
                    val weekendEarnings = record.weekendHours * computedHourlyRate * job.weekendMultiplier
                    val holidayEarnings = record.holidayHours * computedHourlyRate * job.holidayMultiplier

                    if (record.isRestDay) {
                        val restDayBase = if (isFutureDay) 0.0 else baseDailySalary
                        dailyEarnings = restDayBase + weekendEarnings + holidayEarnings + nightEarnings + overtimeEarnings
                    } else {
                        dailyEarnings = workedNormalEarnings + extraNormalEarnings + overtimeEarnings + nightEarnings + weekendEarnings + holidayEarnings
                    }
                } else if (job.employmentType == "YEVMİYELİ" || job.employmentType == "YEVMIYELI") {
                    if (record.netHours > 0) {
                        val baseDailyStdHours = maxOf(1.0, job.dailyWage / computedHourlyRate)
                        val regularHours = minOf(record.normalHours, baseDailyStdHours)
                        val overtimeHours = record.overtimeHours + maxOf(0.0, record.normalHours - baseDailyStdHours)

                        val normalEarnings = regularHours * computedHourlyRate
                        val overtimeEarnings = overtimeHours * computedHourlyRate * job.overtimeMultiplier
                        val nightEarnings = record.nightHours * computedHourlyRate * (job.nightShiftMultiplier - 1.0)
                        val weekendEarnings = record.weekendHours * computedHourlyRate * job.weekendMultiplier
                        val holidayEarnings = record.holidayHours * computedHourlyRate * job.holidayMultiplier

                        dailyEarnings = normalEarnings + overtimeEarnings + nightEarnings + weekendEarnings + holidayEarnings
                    }
                } else {
                    val normalEarnings = record.normalHours * computedHourlyRate
                    val overtimeEarnings = record.overtimeHours * computedHourlyRate * job.overtimeMultiplier
                    val nightEarnings = record.nightHours * computedHourlyRate * (job.nightShiftMultiplier - 1.0)
                    val weekendEarnings = record.weekendHours * computedHourlyRate * job.weekendMultiplier
                    val holidayEarnings = record.holidayHours * computedHourlyRate * job.holidayMultiplier

                    dailyEarnings = normalEarnings + overtimeEarnings + nightEarnings + weekendEarnings + holidayEarnings
                }
            }

            map[dayStart] = record.copy(earnings = dailyEarnings)
        }

        return map
    }

    fun parseTimeInput(input: String): Pair<Int, Int>? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        
        // Check if it's a pure integer
        val pureInt = trimmed.toIntOrNull()
        if (pureInt != null) {
            if (pureInt in 0..24) {
                val hr = if (pureInt == 24) 0 else pureInt
                return hr to 0
            }
            // Could be HHMM format, e.g. 2030 or 930
            if (trimmed.length == 3 || trimmed.length == 4) {
                val min = trimmed.takeLast(2).toIntOrNull() ?: 0
                val hr = trimmed.dropLast(2).toIntOrNull() ?: 0
                if (hr in 0..23 && min in 0..59) {
                    return hr to min
                }
            }
            return null
        }
        
        // Try splitting with common separators like colon, period, space or hyphen
        val separators = listOf(":", ".", " ", "-")
        for (sep in separators) {
            if (trimmed.contains(sep)) {
                val parts = trimmed.split(sep)
                if (parts.size >= 2) {
                    val hr = parts[0].trim().toIntOrNull()
                    val min = parts[1].trim().toIntOrNull()
                    if (hr != null && min != null && hr in 0..23 && min in 0..59) {
                        return hr to min
                    }
                }
            }
        }
        return null
    }
}
