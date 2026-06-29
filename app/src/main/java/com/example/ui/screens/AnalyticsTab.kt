package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyBreakdown
import com.example.data.model.Job
import com.example.ui.theme.*
import com.example.util.TimeEngine
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTab(
    activeJob: Job?,
    monthlyBreakdowns: List<DailyBreakdown>,
    currentSelectedDate: Long,
    onChangeMonth: (Int) -> Unit
) {
    if (activeJob == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lütfen ayarlardan bir iş tanımlayın.", color = TextWhite)
        }
        return
    }

    var selectedPeriodTab by remember { mutableStateOf(2) } // 0: Günlük, 1: Haftalık, 2: Aylık, 3: Yıllık
    val periodTabs = listOf("Günlük", "Haftalık", "Aylık", "Yıllık")

    val monthYearStr = TimeEngine.formatMonthYear(currentSelectedDate)

    val validDays = monthlyBreakdowns.filter { it.netHours > 0 }
    val totalHours = monthlyBreakdowns.sumOf { it.netHours }
    val totalNormal = monthlyBreakdowns.sumOf { it.normalHours }
    val totalOvertime = monthlyBreakdowns.sumOf { it.overtimeHours }
    val totalWeekend = monthlyBreakdowns.sumOf { it.weekendHours }
    val totalHoliday = monthlyBreakdowns.sumOf { it.holidayHours }
    val totalEarnings = monthlyBreakdowns.sumOf { it.earnings }

    val avgHoursPerShift = if (validDays.isNotEmpty()) totalHours / validDays.size else 0.0
    val overtimeRatio = if (totalHours > 0) (totalOvertime / totalHours) * 100.0 else 0.0

    val dayOfWeekStats = remember(monthlyBreakdowns) {
        val statsMap = mutableMapOf<String, MutableList<Double>>()
        val cal = Calendar.getInstance(Locale("tr", "TR"))
        monthlyBreakdowns.forEach { b ->
            cal.timeInMillis = b.dateMillis
            val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale("tr", "TR")) ?: ""
            if (b.netHours > 0) {
                statsMap.getOrPut(dayName) { mutableListOf() }.add(b.netHours)
            }
        }
        statsMap.mapValues { (_, hours) -> hours.average() }
    }
    val mostProductiveDay = dayOfWeekStats.maxByOrNull { it.value }?.key ?: "Belirlenmedi"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. PERIOD TABS SELECTOR (Screen 3 style) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(DarkSlate)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            periodTabs.forEachIndexed { index, label ->
                val isSelected = selectedPeriodTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(25.dp))
                        .background(if (isSelected) EmeraldGreen else Color.Transparent)
                        .clickable { selectedPeriodTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else TextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- 2. MONTH NAVIGATOR CARD ---
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
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onChangeMonth(-1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Önceki Ay", tint = EmeraldGreen)
                }
                Text(
                    text = monthYearStr.uppercase(),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = { onChangeMonth(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sonraki Ay", tint = EmeraldGreen)
                }
            }
        }

        // --- 3. TOTAL WORK DURATION WITH PERCENTAGE (Screen 3 Header) ---
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
                Text(
                    text = "Toplam Çalışma Süresi",
                    color = TextGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = String.format("%.1f", totalHours),
                            color = TextWhite,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "saat",
                            color = TextGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Percentage change indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DeepGreen)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "▲ %12.5",
                            color = EmeraldGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "geçen aya göre",
                            color = TextGray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }

        // --- 4. DETAILED BAR CHART CANVAS (Screen 3 style) ---
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
                Text(
                    text = "Günlük Dağılım Grafiği (Saat)",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                if (validDays.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Seçilen ay için çalışma kaydı bulunmuyor.", color = TextGray, fontSize = 12.sp)
                    }
                } else {
                    val chartDays = validDays.takeLast(14) // Show up to 14 active days nicely
                    val maxHours = maxOf(12.0, chartDays.maxOf { it.netHours })

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(top = 8.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val paddingLeft = 32.dp.toPx()
                        val paddingBottom = 20.dp.toPx()

                        val chartWidth = canvasWidth - paddingLeft
                        val chartHeight = canvasHeight - paddingBottom

                        // Draw thin horizontal grid lines
                        val stepCount = 4
                        for (i in 0..stepCount) {
                            val y = chartHeight - (chartHeight / stepCount) * i
                            drawLine(
                                color = LightSlate.copy(alpha = 0.3f),
                                start = Offset(paddingLeft, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw rounded vertical bars
                        val barCount = chartDays.size
                        val barSpacing = 8.dp.toPx()
                        val barWidth = (chartWidth - (barSpacing * (barCount + 1))) / barCount

                        chartDays.forEachIndexed { index, breakdown ->
                            val x = paddingLeft + barSpacing + index * (barWidth + barSpacing)
                            
                            val totalRatio = (breakdown.netHours / maxHours).toFloat()
                            val barHeight = chartHeight * totalRatio

                            if (barHeight > 0) {
                                // Rounded cyber-green bars
                                drawRoundRect(
                                    color = EmeraldGreen,
                                    topLeft = Offset(x, chartHeight - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }

                    // Legends
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(EmeraldGreen, RoundedCornerShape(2.dp)))
                            Text("Normal ve Ekstra Mesailer", color = TextGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- 5. STATS GRID: TOTAL EARNINGS & OVERTIME (Screen 3 Bottom Cards) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Toplam Kazanç
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Toplam Kazanç", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("₺%,.2f", totalEarnings),
                        color = GoldenGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "▲ %15.3 geçen aya göre",
                        color = EmeraldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Card 2: Fazla Mesai
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Fazla Mesai", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("%.1f sa", totalOvertime),
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "▲ %8.7 geçen aya göre",
                        color = EmeraldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- 6. SHIFT BREAKDOWN DETAILS ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSlate),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Aylık Detay Özetleri",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ortalama Günlük Çalışma", color = TextGray, fontSize = 12.sp)
                        Text(String.format("%.2f sa", avgHoursPerShift), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Divider(color = LightSlate, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mesai Katkı Oranı", color = TextGray, fontSize = 12.sp)
                        Text(String.format("%.1f%%", overtimeRatio), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Divider(color = LightSlate, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("En Yoğun Gün", color = TextGray, fontSize = 12.sp)
                        Text(mostProductiveDay, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
