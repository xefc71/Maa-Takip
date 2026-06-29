package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Job
import com.example.data.model.WorkSession
import com.example.ui.theme.*
import com.example.util.TimeEngine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
    activeJob: Job?,
    sessions: List<WorkSession>,
    onAddSession: (WorkSession) -> Unit,
    onUpdateSession: (WorkSession) -> Unit,
    onDeleteSession: (WorkSession) -> Unit
) {
    if (activeJob == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lütfen ayarlardan bir iş tanımlayın.", color = TextWhite)
        }
        return
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<WorkSession?>(null) }
    var customBreaksList by remember { mutableStateOf(emptyList<com.example.data.model.CustomBreak>()) }
    var showDialogMolaAdder by remember { mutableStateOf(false) }
    var dlgMolaType by remember { mutableStateOf("Çay") }
    var dlgMolaStart by remember { mutableStateOf("10:00") }
    var dlgMolaEnd by remember { mutableStateOf("10:15") }

    var dateInput by remember { mutableStateOf("") }
    var startInput by remember { mutableStateOf("08:00") }
    var endInput by remember { mutableStateOf("18:00") }
    var manualBreakInput by remember { mutableStateOf("0") }
    var noteInput by remember { mutableStateOf("") }
    var isHolidayInput by remember { mutableStateOf(false) }
    var isWeekendInput by remember { mutableStateOf(false) }

    val sdfDisplay = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
    val activeJobSessions = sessions.filter { it.jobId == activeJob.id }

    // Calendar state
    val calendarInstance = remember { Calendar.getInstance(Locale("tr", "TR")) }
    var calendarMonth by remember { mutableStateOf(calendarInstance.clone() as Calendar) }
    var selectedDayTime by remember { mutableStateOf(TimeEngine.getStartOfDay(System.currentTimeMillis())) }

    // Header date display
    val calendarMonthName = remember(calendarMonth) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("tr", "TR"))
        sdf.format(calendarMonth.time)
    }

    // Days list for the grid
    val daysInMonthList = remember(calendarMonth) {
        val list = mutableListOf<CalendarDay>()
        val tempCal = calendarMonth.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        
        // Day of week offset (convert Sunday=1, Monday=2 -> Monday=0, Sunday=6)
        var firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDayOfWeek < 0) firstDayOfWeek = 6 // Sunday is index 6

        // Add dummy dates for preceding month padding
        for (i in 0 until firstDayOfWeek) {
            list.add(CalendarDay(0, false, 0))
        }

        val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (d in 1..maxDay) {
            tempCal.set(Calendar.DAY_OF_MONTH, d)
            val dayStartMillis = TimeEngine.getStartOfDay(tempCal.timeInMillis)
            list.add(CalendarDay(d, true, dayStartMillis))
        }
        list
    }

    // Filter sessions on selected day
    val selectedDaySessions = remember(selectedDayTime, activeJobSessions) {
        activeJobSessions.filter { session ->
            TimeEngine.getStartOfDay(session.startTime) == selectedDayTime
        }
    }

    // Calculations for selected day details
    val totalHoursSelectedDay = selectedDaySessions.sumOf { session ->
        val endTime = session.endTime ?: System.currentTimeMillis()
        val totalMins = ((endTime - session.startTime) / (60 * 1000)).toInt()
        var deduct = 0
        if (activeJob.isBreakAutoDeduct) {
            val startOfDay = TimeEngine.getStartOfDay(session.startTime)
            deduct = ((TimeEngine.getOverlapWithTimeWindow(session.startTime, endTime, activeJob.lunchBreakStart, activeJob.lunchBreakEnd, startOfDay) +
                      TimeEngine.getOverlapWithTimeWindow(session.startTime, endTime, activeJob.coffeeBreakStart, activeJob.coffeeBreakEnd, startOfDay)) / (60 * 1000)).toInt() + activeJob.extraBreakMinutes
        }
        val netMins = maxOf(0, totalMins - deduct - session.manualBreakDurationMinutes)
        netMins / 60.0
    }

    val totalEarningsSelectedDay = selectedDaySessions.sumOf { session ->
        val tempBreakdowns = TimeEngine.calculateDailyBreakdowns(
            activeJob,
            listOf(session),
            selectedDayTime,
            selectedDayTime + 24 * 60 * 60 * 1000L - 1
        )
        tempBreakdowns[selectedDayTime]?.earnings ?: 0.0
    }

    val overtimeSelectedDay = selectedDaySessions.sumOf { session ->
        val tempBreakdowns = TimeEngine.calculateDailyBreakdowns(
            activeJob,
            listOf(session),
            selectedDayTime,
            selectedDayTime + 24 * 60 * 60 * 1000L - 1
        )
        tempBreakdowns[selectedDayTime]?.overtimeHours ?: 0.0
    }

    val manualBreakSelectedDay = selectedDaySessions.sumOf { it.manualBreakDurationMinutes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER TITLE AND MANUAL ADD TRIGGER ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mesaí Takvimi",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        dateInput = sdfDisplay.format(Date(selectedDayTime))
                        startInput = activeJob.standardStartTime
                        endInput = activeJob.standardEndTime
                        manualBreakInput = "0"
                        noteInput = ""
                        isHolidayInput = false
                        isWeekendInput = false
                        customBreaksList = emptyList()
                        showAddDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_manual_session_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manuel Ekle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // --- CALENDAR CONTROL AND GRID CARD (Screen 4 Layout) ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Month controller
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val newCal = calendarMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            calendarMonth = newCal
                        }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Önceki", tint = EmeraldGreen)
                        }

                        Text(
                            text = calendarMonthName.uppercase(),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )

                        IconButton(onClick = {
                            val newCal = calendarMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            calendarMonth = newCal
                        }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sonraki", tint = EmeraldGreen)
                        }
                    }

                    // Weekdays header
                    val weekdays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        weekdays.forEach { dayName ->
                            Text(
                                text = dayName,
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Calendar grid days
                    val chunkedDays = daysInMonthList.chunked(7)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chunkedDays.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                week.forEach { day ->
                                    if (!day.isValid) {
                                        Spacer(modifier = Modifier.size(36.dp))
                                    } else {
                                        val isSelected = day.millis == selectedDayTime
                                        val hasWork = activeJobSessions.any { TimeEngine.getStartOfDay(it.startTime) == day.millis }

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> EmeraldGreen
                                                        hasWork -> DeepGreen.copy(alpha = 0.5f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable { selectedDayTime = day.millis }
                                                .border(
                                                    width = 1.dp,
                                                    color = if (hasWork && !isSelected) EmeraldGreen.copy(alpha = 0.4f) else Color.Transparent,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = day.dayNumber.toString(),
                                                    color = when {
                                                        isSelected -> Color.White
                                                        hasWork -> EmeraldGreen
                                                        else -> TextWhite
                                                    },
                                                    fontWeight = if (isSelected || hasWork) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                                if (hasWork && !isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .background(EmeraldGreen, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SELECTED DAY DETAILED OVERVIEW (Screen 4 Bottom) ---
        item {
            val selectedDateStr = remember(selectedDayTime) {
                val sdf = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr", "TR"))
                sdf.format(Date(selectedDayTime))
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = selectedDateStr,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                if (selectedDaySessions.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSlate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bu tarihte herhangi bir mesai kaydı bulunmamaktadır.",
                                color = TextGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSlate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Primary metrics: Çalışma Süresi & Kazanç
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Toplam Süre", color = TextGray, fontSize = 11.sp)
                                    Text(
                                        text = String.format("%.2f saat", totalHoursSelectedDay),
                                        color = TextWhite,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Kazanılan Hakediş", color = TextGray, fontSize = 11.sp)
                                    Text(
                                        text = String.format("₺%,.2f", totalEarningsSelectedDay),
                                        color = EmeraldGreen,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Divider(color = LightSlate, thickness = 0.5.dp)

                            // Breakdowns Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val firstSession = selectedDaySessions.first()
                                Column {
                                    Text("Başlangıç", color = TextGray, fontSize = 10.sp)
                                    Text(TimeEngine.formatTime(firstSession.startTime), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Column {
                                    Text("Bitiş", color = TextGray, fontSize = 10.sp)
                                    val endText = firstSession.endTime?.let { TimeEngine.formatTime(it) } ?: "Aktif"
                                    Text(endText, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Column {
                                    Text("Mola", color = TextGray, fontSize = 10.sp)
                                    val breakMin = if (activeJob.isBreakAutoDeduct) {
                                        val lunchAndTea = selectedDaySessions.sumOf { session ->
                                            var autoMin = 0
                                            if (activeJob.isBreakAutoDeduct) {
                                                val start = session.startTime
                                                val end = session.endTime ?: System.currentTimeMillis()
                                                val dayStart = TimeEngine.getStartOfDay(start)
                                                var lunchMin = 0
                                                var coffeeMin = 0
                                                if (activeJob.isLunchBreakEnabled) {
                                                    lunchMin = (TimeEngine.getOverlapWithTimeWindow(start, end, activeJob.lunchBreakStart, activeJob.lunchBreakEnd, dayStart) / (60 * 1000)).toInt()
                                                }
                                                if (activeJob.isCoffeeBreakEnabled) {
                                                    coffeeMin = (TimeEngine.getOverlapWithTimeWindow(start, end, activeJob.coffeeBreakStart, activeJob.coffeeBreakEnd, dayStart) / (60 * 1000)).toInt()
                                                }
                                                autoMin = lunchMin + coffeeMin + activeJob.extraBreakMinutes
                                            }
                                            val customBreaks = com.example.data.model.CustomBreak.fromJsonArrayString(session.customBreaksJson)
                                            val customBreakMin = customBreaks.sumOf { it.durationMinutes }
                                            autoMin + session.manualBreakDurationMinutes + customBreakMin
                                        }
                                        lunchAndTea
                                    } else manualBreakSelectedDay
                                    Text("${breakMin} dk", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Column {
                                    Text("Fazla Mesai", color = TextGray, fontSize = 10.sp)
                                    Text(String.format("%.2f sa", overtimeSelectedDay), color = SoftYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SESSIONS DIARY LOG LIST (With Edit & Delete options) ---
        if (selectedDaySessions.isNotEmpty()) {
            item {
                Text(
                    text = "Günlük Mesai Kayıtları",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(selectedDaySessions) { session ->
                val startF = TimeEngine.formatTime(session.startTime)
                val endF = session.endTime?.let { TimeEngine.formatTime(it) } ?: "Devam Ediyor..."
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LightSlate.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("$startF - $endF", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (!session.note.isNullOrBlank()) {
                                Text(session.note, color = TextGray, fontSize = 11.sp, maxLines = 1)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {
                                editingSession = session
                                dateInput = sdfDisplay.format(Date(session.startTime))
                                startInput = TimeEngine.formatTime(session.startTime)
                                endInput = session.endTime?.let { TimeEngine.formatTime(it) } ?: ""
                                manualBreakInput = session.manualBreakDurationMinutes.toString()
                                noteInput = session.note ?: ""
                                isHolidayInput = session.isHoliday
                                isWeekendInput = session.isWeekend
                                showAddDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDeleteSession(session) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = AlertRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG FOR ADD / EDIT MANUAL SESSION ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = DarkSlate,
            title = {
                Text(
                    text = if (editingSession == null) "Manuel Mesai Ekle" else "Mesai Düzenle",
                    color = GoldenGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Tarih (GG.AA.YYYY)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = LightSlate
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = startInput,
                        onValueChange = { startInput = it },
                        label = { Text("Başlangıç Saati (SS:DD)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = LightSlate
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = endInput,
                        onValueChange = { endInput = it },
                        label = { Text("Bitiş Saati (SS:DD)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = LightSlate
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualBreakInput,
                        onValueChange = { manualBreakInput = it },
                        label = { Text("Ek Mola Süresi (Dakika)", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = LightSlate
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Not", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = LightSlate
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(color = LightSlate, modifier = Modifier.padding(vertical = 4.dp))

                    Text("Seans Ek Molaları (Çay, Yemek, İzin)", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    if (customBreaksList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            customBreaksList.forEachIndexed { idx, cb ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LightSlate.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${cb.type} Molası (${cb.startTime} - ${cb.endTime})",
                                        color = TextWhite,
                                        fontSize = 12.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            customBreaksList = customBreaksList.toMutableList().apply { removeAt(idx) }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = AlertRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (showDialogMolaAdder) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightSlate.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Mola Türünü Seçin", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("Çay", "Yemek", "İzin").forEach { t ->
                                        val isSel = dlgMolaType == t
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSel) EmeraldGreen else LightSlate, RoundedCornerShape(6.dp))
                                                .clickable { dlgMolaType = t }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(t, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = dlgMolaStart,
                                        onValueChange = { dlgMolaStart = it },
                                        label = { Text("Başlangıç", color = TextGray, fontSize = 10.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedBorderColor = EmeraldGreen,
                                            unfocusedBorderColor = LightSlate
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = dlgMolaEnd,
                                        onValueChange = { dlgMolaEnd = it },
                                        label = { Text("Bitiş", color = TextGray, fontSize = 10.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedBorderColor = EmeraldGreen,
                                            unfocusedBorderColor = LightSlate
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showDialogMolaAdder = false }) {
                                        Text("Vazgeç", color = TextGray, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(
                                        onClick = {
                                            val sMin = TimeEngine.parseTimeToMinutes(dlgMolaStart)
                                            val eMin = TimeEngine.parseTimeToMinutes(dlgMolaEnd)
                                            var d = eMin - sMin
                                            if (d < 0) d += 1440
                                            val nb = com.example.data.model.CustomBreak(dlgMolaType, dlgMolaStart, dlgMolaEnd, d)
                                            customBreaksList = customBreaksList + nb
                                            showDialogMolaAdder = false
                                        }
                                    ) {
                                        Text("Mola Ekle", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { showDialogMolaAdder = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Yeni Mola Kaydı Ekle", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Divider(color = LightSlate, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isHolidayInput,
                            onCheckedChange = { isHolidayInput = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                        )
                        Text("Resmi Tatil Mesaisi", color = TextWhite, fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isWeekendInput,
                            onCheckedChange = { isWeekendInput = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                        )
                        Text("Hafta Sonu Mesaisi", color = TextWhite, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedDate = try {
                            SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR")).parse(dateInput)
                        } catch (e: Exception) {
                            null
                        }

                        if (parsedDate != null) {
                            val startCal = Calendar.getInstance().apply {
                                time = parsedDate
                                val split = startInput.split(":")
                                set(Calendar.HOUR_OF_DAY, split.getOrNull(0)?.toIntOrNull() ?: 8)
                                set(Calendar.MINUTE, split.getOrNull(1)?.toIntOrNull() ?: 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val endCal = Calendar.getInstance().apply {
                                time = parsedDate
                                val split = endInput.split(":")
                                set(Calendar.HOUR_OF_DAY, split.getOrNull(0)?.toIntOrNull() ?: 18)
                                set(Calendar.MINUTE, split.getOrNull(1)?.toIntOrNull() ?: 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            if (endCal.before(startCal)) {
                                endCal.add(Calendar.DAY_OF_MONTH, 1)
                            }

                            val newSession = WorkSession(
                                id = editingSession?.id ?: 0,
                                jobId = activeJob.id,
                                startTime = startCal.timeInMillis,
                                endTime = endCal.timeInMillis,
                                manualBreakDurationMinutes = manualBreakInput.toIntOrNull() ?: 0,
                                note = noteInput.ifBlank { null },
                                isHoliday = isHolidayInput,
                                isWeekend = isWeekendInput,
                                customBreaksJson = com.example.data.model.CustomBreak.toJsonArrayString(customBreaksList)
                            )

                            if (editingSession == null) {
                                onAddSession(newSession)
                            } else {
                                onUpdateSession(newSession)
                            }

                            showAddDialog = false
                            editingSession = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        editingSession = null
                    }
                ) {
                    Text("Vazgeç", color = TextGray)
                }
            }
        )
    }
}

// Helper model to model calendar days
data class CalendarDay(
    val dayNumber: Int,
    val isValid: Boolean,
    val millis: Long
)
