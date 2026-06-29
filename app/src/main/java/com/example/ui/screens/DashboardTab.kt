package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyBreakdown
import com.example.data.model.Job
import com.example.data.model.WorkSession
import com.example.ui.theme.*
import com.example.util.TimeEngine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(
    activeJob: Job?,
    activeSession: WorkSession?,
    monthlyBreakdowns: List<DailyBreakdown>,
    liveTickerTime: Long,
    onStartWork: (Int) -> Unit,
    onEndWork: (String?, Int, Boolean, Boolean) -> Unit,
    allJobs: List<Job>,
    onSelectJob: (Int) -> Unit,
    onUpdateActiveSession: ((WorkSession) -> Unit)? = null
) {
    var showEndWorkDialog by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }
    var manualBreakInput by remember { mutableStateOf("0") }
    var isHolidayOverride by remember { mutableStateOf(false) }
    var isWeekendOverride by remember { mutableStateOf(false) }

    var showAddCustomBreakDialog by remember { mutableStateOf(false) }
    var selectedBreakType by remember { mutableStateOf("Çay") }
    var breakStartInput by remember { mutableStateOf("10:00") }
    var breakEndInput by remember { mutableStateOf("10:15") }

    if (activeJob == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lütfen ayarlardan bir iş tanımlayın.", color = TextWhite)
        }
        return
    }

    val totalHoursThisMonth = monthlyBreakdowns.sumOf { it.netHours }
    val totalOvertimeThisMonth = monthlyBreakdowns.sumOf { it.overtimeHours }
    val totalEarningsThisMonth = monthlyBreakdowns.sumOf { it.earnings }

    val calendar = Calendar.getInstance(Locale("tr", "TR"))
    val currentWeekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
    val weeklyBreakdowns = monthlyBreakdowns.filter { breakdown ->
        val cal = Calendar.getInstance(Locale("tr", "TR"))
        cal.timeInMillis = breakdown.dateMillis
        cal.get(Calendar.WEEK_OF_YEAR) == currentWeekOfYear
    }
    val totalHoursThisWeek = weeklyBreakdowns.sumOf { it.netHours }

    val isSessionActive = activeSession != null
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var liveEarningsToday by remember { mutableStateOf(0.0) }

    LaunchedEffect(key1 = liveTickerTime, key2 = activeSession) {
        if (activeSession != null) {
            val diff = maxOf(0L, liveTickerTime - activeSession.startTime)
            elapsedSeconds = diff / 1000L

            val tempSession = activeSession.copy(endTime = liveTickerTime)
            val dayStart = TimeEngine.getStartOfDay(activeSession.startTime)
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L - 1
            val temporaryBreakdowns = TimeEngine.calculateDailyBreakdowns(
                activeJob,
                listOf(tempSession),
                dayStart,
                dayEnd
            )
            val todayBreakdown = temporaryBreakdowns[dayStart]
            liveEarningsToday = todayBreakdown?.earnings ?: 0.0
        } else {
            elapsedSeconds = 0L
            liveEarningsToday = 0.0
        }
    }

    val durationHours = elapsedSeconds / 3600
    val durationMinutes = (elapsedSeconds % 3600) / 60
    val durationSecs = elapsedSeconds % 60
    val formattedDuration = String.format("%02d:%02d:%02d", durationHours, durationMinutes, durationSecs)

    val todayStartMillis = TimeEngine.getStartOfDay(System.currentTimeMillis())
    val todayBreakdown = monthlyBreakdowns.find { it.dateMillis == todayStartMillis }
    val staticEarningsToday = todayBreakdown?.earnings ?: 0.0
    val displayEarningsToday = if (isSessionActive) liveEarningsToday else staticEarningsToday
    val displayHoursToday = if (isSessionActive) {
        val tempSession = activeSession.copy(endTime = liveTickerTime)
        val split = TimeEngine.splitSessionByDay(tempSession)
        val mins = split.sumOf { (start, end) ->
            val totalMins = ((end - start) / (60 * 1000)).toInt()
            var deduct = 0
            if (activeJob.isBreakAutoDeduct) {
                val lunchOverlap = if (activeJob.isLunchBreakEnabled) {
                    TimeEngine.getOverlapWithTimeWindow(start, end, activeJob.lunchBreakStart, activeJob.lunchBreakEnd, todayStartMillis)
                } else 0L
                val coffeeOverlap = if (activeJob.isCoffeeBreakEnabled) {
                    TimeEngine.getOverlapWithTimeWindow(start, end, activeJob.coffeeBreakStart, activeJob.coffeeBreakEnd, todayStartMillis)
                } else 0L
                deduct = ((lunchOverlap + coffeeOverlap) / (60 * 1000)).toInt() + activeJob.extraBreakMinutes
            }
            val customBreaks = com.example.data.model.CustomBreak.fromJsonArrayString(activeSession.customBreaksJson)
            val customBreakMin = customBreaks.sumOf { it.durationMinutes }
            maxOf(0, totalMins - deduct - customBreakMin)
        }
        mins / 60.0
    } else {
        todayBreakdown?.netHours ?: 0.0
    }

    // Format displayHoursToday to HH:MM:SS format
    val todayTotalSeconds = (displayHoursToday * 3600).toLong()
    val todayH = todayTotalSeconds / 3600
    val todayM = (todayTotalSeconds % 3600) / 60
    val todayS = todayTotalSeconds % 60
    val formattedTodayDuration = String.format("%02d:%02d:%02d", todayH, todayM, todayS)

    val turkishDateStr = remember(liveTickerTime) {
        val sdf = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr", "TR"))
        sdf.format(Date())
    }

    // Infinite transition for glowing active dots
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                0.3f at 0
                1.0f at 750
                0.3f at 1500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HEADER SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Merhaba, ${activeJob.name.split(" ").firstOrNull() ?: "Kullanıcı"} 👋",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bugün $turkishDateStr",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Theme Toggle & Notification / Profile Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme Toggle Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSlate)
                        .border(1.dp, LightSlate, CircleShape)
                        .clickable {
                            ThemeState.isDark = !ThemeState.isDark
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (ThemeState.isDark) "☀️" else "🌙",
                        fontSize = 18.sp
                    )
                }

                // Notification / Profile Indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSlate)
                        .border(1.dp, LightSlate, CircleShape)
                        .clickable { /* action */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Bildirimler",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 2. MULTI-JOB PROFILE CHANGER ---
        if (allJobs.size > 1) {
            var expanded by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LightSlate, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(10.dp).background(EmeraldGreen, CircleShape))
                        Text("Aktif İş Profili:", color = TextGray, fontSize = 12.sp)
                    }
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeepGreen)
                                .clickable { expanded = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(activeJob.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(DarkSlate)
                        ) {
                            allJobs.forEach { job ->
                                DropdownMenuItem(
                                    text = { Text(job.name, color = TextWhite) },
                                    onClick = {
                                        onSelectJob(job.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isSessionActive) {
            // ==================== SCREEN 1: STATIC DASHBOARD ====================
            
            // BUGÜN CARD
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "BUGÜN",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Çalışma Süresi", color = TextGray, fontSize = 12.sp)
                            Text(
                                text = formattedTodayDuration,
                                color = TextWhite,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("Saat : Dakika : Saniye", color = TextGray.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Bugünkü Kazanç", color = TextGray, fontSize = 12.sp)
                            Text(
                                text = String.format("₺%,.2f", displayEarningsToday),
                                color = EmeraldGreen,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Tahmini Hakediş", color = TextGray.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }
            }

            // WEEK & MONTH DUAL COLUMN ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BU HAFTA
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Bu Hafta", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("Toplam Süre", color = TextGray, fontSize = 10.sp)
                                Text(
                                    text = String.format("%.1f sa", totalHoursThisWeek),
                                    color = TextWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Mini decorative bar chart
                            Canvas(modifier = Modifier.size(width = 50.dp, height = 30.dp)) {
                                val barWidth = 5.dp.toPx()
                                val gap = 3.dp.toPx()
                                val heights = listOf(0.3f, 0.6f, 0.4f, 0.8f, 0.5f, 0.9f)
                                heights.forEachIndexed { i, h ->
                                    val x = i * (barWidth + gap)
                                    val y = size.height - (size.height * h)
                                    drawRoundRect(
                                        color = EmeraldGreen,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, size.height * h),
                                        cornerRadius = CornerRadius(2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                // BU AY
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Bu Ay", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("Toplam Süre", color = TextGray, fontSize = 10.sp)
                                Text(
                                    text = String.format("%.1f sa", totalHoursThisMonth),
                                    color = TextWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Mini decorative weekly bar chart
                            Canvas(modifier = Modifier.size(width = 50.dp, height = 30.dp)) {
                                val barWidth = 7.dp.toPx()
                                val gap = 4.dp.toPx()
                                val heights = listOf(0.4f, 0.7f, 0.9f, 0.6f)
                                heights.forEachIndexed { i, h ->
                                    val x = i * (barWidth + gap)
                                    val y = size.height - (size.height * h)
                                    drawRoundRect(
                                        color = GoldenGold,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, size.height * h),
                                        cornerRadius = CornerRadius(2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MONTHLY ESTIMATED SALARY WITH BEZIER GRAPH
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Aylık Tahmini Maaş", color = TextGray, fontSize = 12.sp)
                            Text(
                                text = String.format("₺%,.2f", totalEarningsThisMonth),
                                color = GoldenGold,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Mini glowing bezier wave
                        Canvas(modifier = Modifier.size(width = 90.dp, height = 35.dp)) {
                            val path = Path().apply {
                                moveTo(0f, size.height * 0.8f)
                                cubicTo(
                                    size.width * 0.25f, size.height * 0.9f,
                                    size.width * 0.5f, size.height * 0.2f,
                                    size.width * 0.75f, size.height * 0.3f
                                )
                                lineTo(size.width, size.height * 0.1f)
                            }
                            drawPath(
                                path = path,
                                color = EmeraldGreen,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }

            // EXTRA OVERTIME OVERVIEW
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SoftYellow, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Fazla Mesai (Bu Ay)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Maaşınıza eklenen fazla mesai saatleri", color = TextGray, fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = String.format("%.1f sa", totalOvertimeThisMonth),
                        color = SoftYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // START WORK TRIGGER BUTTON
            Button(
                onClick = { onStartWork(activeJob.id) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_stop_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Mesaiyi Başlat", tint = Color.White)
                    Text("ÇALIŞMAYI BAŞLAT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                }
            }

        } else {
            // ==================== SCREEN 2: ACTIVE TRACKING WORK ====================
            
            // STATUS LINE WITH PULSATING GLOW
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(DeepGreen)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .drawBehind {
                            drawCircle(
                                color = EmeraldGreen,
                                radius = size.minDimension / 2,
                                alpha = pulseAlpha
                            )
                        }
                )
                Text(
                    text = "ÇALIŞMA DEVAM EDİYOR",
                    color = EmeraldGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // BIG CIRCULAR TIMER VIEW
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring glowing stroke
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx())
                    )
                    // Active animated ring segment
                    val sweep = (elapsedSeconds % 60) * 6f
                    drawArc(
                        color = EmeraldGreen,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }

                // Digital time display inside ring
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Çalışma Süresi",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDuration,
                        color = TextWhite,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saat : Dakika : Saniye",
                        color = TextGray.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Başlangıç: ${TimeEngine.formatTime(activeSession.startTime)}",
                        color = EmeraldGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // DYNAMIC METRICS GRID UNDER CIRCULAR PROGRESS
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Column 1: Net Süre
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Net Süre", color = TextGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.2f sa", displayHoursToday),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(LightSlate))

                    // Column 2: Mola Süresi
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mola Süresi", color = TextGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val customBreaks = if (isSessionActive && activeSession != null) {
                            com.example.data.model.CustomBreak.fromJsonArrayString(activeSession.customBreaksJson)
                        } else emptyList()
                        val customBreakMin = customBreaks.sumOf { it.durationMinutes }
                        val molaMin = (if (activeJob.isBreakAutoDeduct) {
                            val lunchMin = if (activeJob.isLunchBreakEnabled) 60 else 0
                            val coffeeMin = if (activeJob.isCoffeeBreakEnabled) 15 else 0
                            lunchMin + coffeeMin + activeJob.extraBreakMinutes
                        } else 0) + customBreakMin
                        Text(
                            text = "${molaMin} dk",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(LightSlate))

                    // Column 3: Fazla Mesai
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Fazla Mesai", color = TextGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.2f sa", displayHoursToday - 8.0).let { if (displayHoursToday > 8.0) it else "0.00 sa" },
                            color = SoftYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // CRITICAL STOP WORK BUTTON
            Button(
                onClick = {
                    noteInput = ""
                    manualBreakInput = "0"
                    isHolidayOverride = false
                    isWeekendOverride = false
                    showEndWorkDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_stop_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Çalışmayı Bitir", tint = Color.White)
                    Text("ÇALIŞMAYI BİTİR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                }
            }
        }

        // AUTO-BREAK INDICATOR BADGE
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DeepGreen.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("☕", fontSize = 20.sp)
                    Column {
                        Text("Tanımlı Otomatik Molalar", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val lunchText = if (activeJob.isLunchBreakEnabled) "Öğle: ${activeJob.lunchBreakStart} - ${activeJob.lunchBreakEnd}" else "Öğle: Kapalı"
                        val coffeeText = if (activeJob.isCoffeeBreakEnabled) "Çay: ${activeJob.coffeeBreakStart} - ${activeJob.coffeeBreakEnd}" else "Çay: Kapalı"
                        Text(
                            text = "$lunchText | $coffeeText" +
                                   (if (activeJob.extraBreakMinutes > 0) " | Ekstra: ${activeJob.extraBreakMinutes} dk" else ""),
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                }
                
                if (isSessionActive) {
                    IconButton(
                        onClick = { showAddCustomBreakDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(EmeraldGreen, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ek Mola Ekle",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (isSessionActive && activeSession != null) {
            val customBreaks = remember(activeSession) {
                com.example.data.model.CustomBreak.fromJsonArrayString(activeSession.customBreaksJson)
            }
            if (customBreaks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LightSlate, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Eklenen Seans Molaları",
                            color = GoldenGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        customBreaks.forEach { cb ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                when (cb.type) {
                                                    "Çay" -> EmeraldGreen
                                                    "Yemek" -> SoftYellow
                                                    else -> AlertRed
                                                },
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = "${cb.type} Molası (${cb.startTime} - ${cb.endTime})",
                                        color = TextWhite,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "${cb.durationMinutes} dk",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- ADD CUSTOM BREAK DIALOG ---
        if (showAddCustomBreakDialog && activeSession != null) {
            AlertDialog(
                onDismissRequest = { showAddCustomBreakDialog = false },
                containerColor = DarkSlate,
                title = { Text("Ek Mola Ekle", color = GoldenGold, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mola Türünü Seçin", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Çay" to "1. Çay",
                                "Yemek" to "2. Yemek",
                                "İzin" to "3. İzin"
                            ).forEach { (typeKey, typeLabel) ->
                                val isSelected = selectedBreakType == typeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) DeepGreen else LightSlate)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) EmeraldGreen else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedBreakType = typeKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = typeLabel,
                                        color = if (isSelected) EmeraldGreen else TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = breakStartInput,
                                onValueChange = { breakStartInput = it },
                                label = { Text("Başlangıç (SS:DD)", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = LightSlate
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = breakEndInput,
                                onValueChange = { breakEndInput = it },
                                label = { Text("Bitiş (SS:DD)", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = LightSlate
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val startMin = TimeEngine.parseTimeToMinutes(breakStartInput)
                            val endMin = TimeEngine.parseTimeToMinutes(breakEndInput)
                            var diff = endMin - startMin
                            if (diff < 0) {
                                diff += 1440
                            }
                            
                            val newBreak = com.example.data.model.CustomBreak(
                                type = selectedBreakType,
                                startTime = breakStartInput,
                                endTime = breakEndInput,
                                durationMinutes = diff
                            )
                            
                            val currentBreaks = com.example.data.model.CustomBreak.fromJsonArrayString(activeSession.customBreaksJson).toMutableList()
                            currentBreaks.add(newBreak)
                            
                            val updatedSession = activeSession.copy(
                                customBreaksJson = com.example.data.model.CustomBreak.toJsonArrayString(currentBreaks)
                            )
                            onUpdateActiveSession?.invoke(updatedSession)
                            
                            showAddCustomBreakDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Ekle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomBreakDialog = false }) {
                        Text("Vazgeç", color = TextGray)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- SAVE WORK DIALOG ---
    if (showEndWorkDialog) {
        AlertDialog(
            onDismissRequest = { showEndWorkDialog = false },
            containerColor = DarkSlate,
            title = { Text("Mesaiyi Kaydet", color = GoldenGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kaydetmeden önce mesai detaylarını gözden geçirin:", color = TextWhite, fontSize = 14.sp)

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Çalışma Notu (İsteğe bağlı)", color = TextGray) },
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isHolidayOverride,
                            onCheckedChange = { isHolidayOverride = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                        )
                        Text("Bugün Resmi Tatil", color = TextWhite, fontSize = 14.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isWeekendOverride,
                            onCheckedChange = { isWeekendOverride = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                        )
                        Text("Bugün Hafta Sonu Mesaisi", color = TextWhite, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val extraBreak = manualBreakInput.toIntOrNull() ?: 0
                        onEndWork(noteInput.ifBlank { null }, extraBreak, isHolidayOverride, isWeekendOverride)
                        showEndWorkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndWorkDialog = false }) {
                    Text("Vazgeç", color = TextGray)
                }
            }
        )
    }
}
