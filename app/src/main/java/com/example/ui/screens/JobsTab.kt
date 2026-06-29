package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyBreakdown
import com.example.data.model.Job
import com.example.ui.theme.*
import com.example.util.TimeEngine
import com.example.util.NotificationHelper
import android.widget.Toast
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsTab(
    allJobs: List<Job>,
    activeJob: Job?,
    monthlyBreakdowns: List<DailyBreakdown>,
    onAddJob: (Job) -> Unit,
    onUpdateJob: (Job) -> Unit,
    onDeleteJob: (Job) -> Unit,
    onSelectJob: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAddJobDialog by remember { mutableStateOf(false) }
    var editingJob by remember { mutableStateOf<Job?>(null) }

    var jobName by remember { mutableStateOf("") }
    var employmentType by remember { mutableStateOf("MAASLI") }
    var monthlySalaryStr by remember { mutableStateOf("") }
    var dailyWageStr by remember { mutableStateOf("") }
    var hourlyRateStr by remember { mutableStateOf("") }
    var standardStartStr by remember { mutableStateOf("08:00") }
    var standardEndStr by remember { mutableStateOf("18:00") }
    var overtimeMultStr by remember { mutableStateOf("1.5") }
    var nightShiftMultStr by remember { mutableStateOf("1.25") }
    var holidayMultStr by remember { mutableStateOf("2.0") }
    var isBreakAutoDeduct by remember { mutableStateOf(true) }
    var extraBreakMinutesStr by remember { mutableStateOf("0") }
    var isLunchBreakEnabled by remember { mutableStateOf(true) }
    var isCoffeeBreakEnabled by remember { mutableStateOf(true) }
    var lunchStartStr by remember { mutableStateOf("12:00") }
    var lunchEndStr by remember { mutableStateOf("13:00") }
    var coffeeStartStr by remember { mutableStateOf("10:00") }
    var coffeeEndStr by remember { mutableStateOf("10:15") }

    if (activeJob == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lütfen ayarlardan bir iş tanımlayın.", color = TextWhite)
        }
        return
    }

    // Calculations for Summary
    val totalHoursThisMonth = monthlyBreakdowns.sumOf { it.netHours }
    val totalEarningsThisMonth = monthlyBreakdowns.sumOf { it.earnings }
    val totalOvertimeThisMonth = monthlyBreakdowns.sumOf { it.overtimeHours }
    val validDays = monthlyBreakdowns.filter { it.netHours > 0 }
    val avgHoursPerShift = if (validDays.isNotEmpty()) totalHoursThisMonth / validDays.size else 0.0

    // Progress percentage
    val targetMonthlyHours = 160.0
    val progressPercent = ((totalHoursThisMonth / targetMonthlyHours) * 100).coerceAtMost(100.0).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SCREEN 6: PROFIL & ÖZET SECTION ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circle avatar placeholder with silhouette
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkSlate)
                    .border(1.5.dp, EmeraldGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeJob.name,
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (activeJob.employmentType) {
                        "MAASLI" -> "Maaşlı Çalışan"
                        "YEVMİYELİ" -> "Yevmiyeli Çalışan"
                        else -> "Saatlik Ücretli"
                    },
                    color = EmeraldGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Quick Edit active profile settings
            IconButton(
                onClick = {
                    editingJob = activeJob
                    jobName = activeJob.name
                    employmentType = activeJob.employmentType
                    monthlySalaryStr = activeJob.monthlySalary.toString()
                    dailyWageStr = activeJob.dailyWage.toString()
                    hourlyRateStr = activeJob.hourlyRate.toString()
                    standardStartStr = activeJob.standardStartTime
                    standardEndStr = activeJob.standardEndTime
                    overtimeMultStr = activeJob.overtimeMultiplier.toString()
                    nightShiftMultStr = activeJob.nightShiftMultiplier.toString()
                    holidayMultStr = activeJob.holidayMultiplier.toString()
                    isBreakAutoDeduct = activeJob.isBreakAutoDeduct
                    isLunchBreakEnabled = activeJob.isLunchBreakEnabled
                    isCoffeeBreakEnabled = activeJob.isCoffeeBreakEnabled
                    lunchStartStr = activeJob.lunchBreakStart
                    lunchEndStr = activeJob.lunchBreakEnd
                    coffeeStartStr = activeJob.coffeeBreakStart
                    coffeeEndStr = activeJob.coffeeBreakEnd
                    extraBreakMinutesStr = activeJob.extraBreakMinutes.toString()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSlate)
                    .border(1.dp, LightSlate, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Profili Düzenle",
                    tint = GoldenGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // --- ÖZET BİLGİLER CARD (Screen 6 Details Table) ---
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
                    text = "Özet Bilgiler",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: Aylık Maaş / Ücret
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Aylık Taban Maaş/Ücret", color = TextGray, fontSize = 12.sp)
                        Text(
                            text = when (activeJob.employmentType) {
                                "MAASLI" -> String.format("₺%,.2f", activeJob.monthlySalary)
                                "YEVMİYELİ" -> String.format("₺%,.2f (Günlük)", activeJob.dailyWage)
                                else -> String.format("₺%,.2f (Saatlik)", activeJob.hourlyRate)
                            },
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Divider(color = LightSlate, thickness = 0.5.dp)

                    // Row 2: Aylık Tahmini Kazanç
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Aylık Tahmini Kazanç", color = TextGray, fontSize = 12.sp)
                        Text(
                            text = String.format("₺%,.2f", totalEarningsThisMonth),
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Divider(color = LightSlate, thickness = 0.5.dp)

                    // Row 3: Toplam Çalışma Süresi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Toplam Çalışma Süresi", color = TextGray, fontSize = 12.sp)
                        Text(
                            text = String.format("%.1f saat", totalHoursThisMonth),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Divider(color = LightSlate, thickness = 0.5.dp)

                    // Row 4: Fazla Mesai
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fazla Mesai", color = TextGray, fontSize = 12.sp)
                        Text(
                            text = String.format("%.1f saat", totalOvertimeThisMonth),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Divider(color = LightSlate, thickness = 0.5.dp)

                    // Row 5: Ortalama Günlük Süre
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ortalama Günlük Süre", color = TextGray, fontSize = 12.sp)
                        Text(
                            text = String.format("%.2f saat", avgHoursPerShift),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Indicator
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bu Ayki İlerleme", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("%$progressPercent", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    LinearProgressIndicator(
                        progress = { (totalHoursThisMonth / targetMonthlyHours).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldGreen,
                        trackColor = LightSlate
                    )
                    
                    Text(
                        text = "Hedef: ${targetMonthlyHours.toInt()} saat | Tamamlanan: ${String.format("%.1f", totalHoursThisMonth)} saat",
                        color = TextGray,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        // --- SCREEN 5: ÇALIŞMA AYARLARI LIST SECTION ---
        Text(
            text = "Çalışma Ayarları",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSlate),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val settingsRows = listOf(
                    SettingsItem("Çalışma Saatleri", "${activeJob.standardStartTime} - ${activeJob.standardEndTime}", Icons.Default.DateRange),
                    SettingsItem("Mola Ayarları", if (activeJob.isBreakAutoDeduct) "Öğle ve Çay Molası Aktif" else "Kapalı", Icons.Default.Favorite),
                    SettingsItem("Fazla Mesai Çarpanı", "${activeJob.overtimeMultiplier}x", Icons.Default.Star),
                    SettingsItem("Gece Vardiyası Çarpanı", "${activeJob.nightShiftMultiplier}x", Icons.Default.Build),
                    SettingsItem("Tatil & Resmi Gün Çarpanı", "${activeJob.holidayMultiplier}x", Icons.Default.Info)
                )

                settingsRows.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Trigger edit
                                editingJob = activeJob
                                jobName = activeJob.name
                                employmentType = activeJob.employmentType
                                monthlySalaryStr = activeJob.monthlySalary.toString()
                                dailyWageStr = activeJob.dailyWage.toString()
                                hourlyRateStr = activeJob.hourlyRate.toString()
                                standardStartStr = activeJob.standardStartTime
                                standardEndStr = activeJob.standardEndTime
                                overtimeMultStr = activeJob.overtimeMultiplier.toString()
                                nightShiftMultStr = activeJob.nightShiftMultiplier.toString()
                                holidayMultStr = activeJob.holidayMultiplier.toString()
                                isBreakAutoDeduct = activeJob.isBreakAutoDeduct
                                isLunchBreakEnabled = activeJob.isLunchBreakEnabled
                                isCoffeeBreakEnabled = activeJob.isCoffeeBreakEnabled
                                lunchStartStr = activeJob.lunchBreakStart
                                lunchEndStr = activeJob.lunchBreakEnd
                                coffeeStartStr = activeJob.coffeeBreakStart
                                coffeeEndStr = activeJob.coffeeBreakEnd
                                extraBreakMinutesStr = activeJob.extraBreakMinutes.toString()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Circle green icon wrapper
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(DeepGreen.copy(alpha = 0.5f), CircleShape)
                                    .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(item.label, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.value, color = TextGray, fontSize = 12.sp)
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (index < settingsRows.size - 1) {
                        Divider(color = LightSlate, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
                    }
                }
            }
        }

        // --- MULTI-JOB PROFILE LIST SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Tüm İş Profilleriniz",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            TextButton(
                onClick = {
                    jobName = "Yeni İş Profili"
                    employmentType = "MAASLI"
                    monthlySalaryStr = "35000"
                    dailyWageStr = "1500"
                    hourlyRateStr = "200"
                    standardStartStr = "08:00"
                    standardEndStr = "18:00"
                    overtimeMultStr = "1.5"
                    nightShiftMultStr = "1.25"
                    holidayMultStr = "2.0"
                    isBreakAutoDeduct = true
                    isLunchBreakEnabled = true
                    isCoffeeBreakEnabled = true
                    lunchStartStr = "12:00"
                    lunchEndStr = "13:00"
                    coffeeStartStr = "10:00"
                    coffeeEndStr = "10:15"
                    extraBreakMinutesStr = "0"
                    showAddJobDialog = true
                }
            ) {
                Text("+ Ekle", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            allJobs.forEach { job ->
                val isCurrent = job.id == activeJob.id
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isCurrent) EmeraldGreen else LightSlate.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(job.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DeepGreen)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("AKTİF", color = EmeraldGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = when (job.employmentType) {
                                    "MAASLI" -> "Aylık: ₺${job.monthlySalary}"
                                    "YEVMİYELİ" -> "Günlük: ₺${job.dailyWage}"
                                    else -> "Saatlik: ₺${job.hourlyRate}"
                                },
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!isCurrent) {
                                IconButton(onClick = { onSelectJob(job.id) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Profili Değiştir", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                }
                                if (allJobs.size > 1) {
                                    IconButton(onClick = { onDeleteJob(job) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Profili Sil", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SHARING & EXPORT SECTION ---
        if (monthlyBreakdowns.isNotEmpty()) {
            Text(
                "Veri Aktarımı & Raporlama",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Detaylı hakediş raporlarını telefonunuzda paylaşın:", color = TextGray, fontSize = 12.sp)

                    Button(
                        onClick = {
                            val report = generateTextReport(activeJob, monthlyBreakdowns)
                            shareReport(context, report, "MesaiveMaas_Raporu.txt")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Metin Raporu Paylaş", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val csvReport = generateCsvReport(monthlyBreakdowns)
                            shareReport(context, csvReport, "Hakedis_Tablosu.csv")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excel / CSV Paylaş", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- BİLDİRİM TEST PANELİ SECTION ---
        Text(
            "🔔 Günlük Bildirim Testi",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSlate),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LightSlate, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Günün farklı saatlerinde gelecek olan tüm bildirimleri (7 adet) sırasıyla göndererek test edin. Her bildirim özel başlığı, emojileri ve butonlarıyla birlikte status bar'da görünecektir.",
                    color = TextGray,
                    fontSize = 12.sp
                )

                var isTestingNotifications by remember { mutableStateOf(false) }

                Button(
                    onClick = {
                        if (!isTestingNotifications) {
                            isTestingNotifications = true
                            coroutineScope.launch {
                                val ids = listOf(1001, 1002, 1003, 1004, 1005, 1006, 1007)
                                val hourLabels = listOf("07:45", "08:00", "08:10", "12:00", "17:45", "18:00", "22:00")
                                for (i in ids.indices) {
                                    val id = ids[i]
                                    val timeLabel = hourLabels[i]
                                    NotificationHelper.showReminderNotification(context, id)
                                    Toast.makeText(context, "$timeLabel bildirimi gönderildi!", Toast.LENGTH_SHORT).show()
                                    kotlinx.coroutines.delay(1200)
                                }
                                isTestingNotifications = false
                                Toast.makeText(context, "✅ Tüm bildirim testleri tamamlandı!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTestingNotifications
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTestingNotifications) "Test Ediliyor..." else "Sırasıyla Bildirimleri Test Et",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- ADD / EDIT PROFILE SETTINGS DIALOG ---
    if (showAddJobDialog || editingJob != null) {
        val isEditing = editingJob != null
        AlertDialog(
            onDismissRequest = {
                showAddJobDialog = false
                editingJob = null
            },
            containerColor = DarkSlate,
            title = {
                Text(
                    text = if (isEditing) "Profil Ayarlarını Düzenle" else "Yeni İş Profili Ekle",
                    color = GoldenGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = jobName,
                        onValueChange = { jobName = it },
                        label = { Text("İş Adı / Profil İsmi", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = LightSlate
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Görüş / Gelir Türü", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "MAASLI" to "Maaşlı",
                            "YEVMİYELİ" to "Yevmiye",
                            "SAAT_ÜCRETLİ" to "Saatlik"
                        ).forEach { (key, label) ->
                            val isSel = employmentType == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DeepGreen else LightSlate)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) EmeraldGreen else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { employmentType = key }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (isSel) EmeraldGreen else TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    when (employmentType) {
                        "MAASLI" -> {
                            OutlinedTextField(
                                value = monthlySalaryStr,
                                onValueChange = { monthlySalaryStr = it },
                                label = { Text("Aylık Net Maaş (TL)", color = TextGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = LightSlate
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "YEVMİYELİ" -> {
                            OutlinedTextField(
                                value = dailyWageStr,
                                onValueChange = { dailyWageStr = it },
                                label = { Text("Günlük Net Yevmiye (TL)", color = TextGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = LightSlate
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            OutlinedTextField(
                                value = hourlyRateStr,
                                onValueChange = { hourlyRateStr = it },
                                label = { Text("Saatlik Net Ücret (TL)", color = TextGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = LightSlate
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = standardStartStr,
                            onValueChange = { standardStartStr = it },
                            label = { Text("Giriş (SS:DD)", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = LightSlate
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = standardEndStr,
                            onValueChange = { standardEndStr = it },
                            label = { Text("Çıkış (SS:DD)", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = LightSlate
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = overtimeMultStr,
                        onValueChange = { overtimeMultStr = it },
                        label = { Text("Fazla Mesai Çarpanı", color = TextGray) },
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
                        value = nightShiftMultStr,
                        onValueChange = { nightShiftMultStr = it },
                        label = { Text("Gece Vardiyası Çarpanı", color = TextGray) },
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
                        value = holidayMultStr,
                        onValueChange = { holidayMultStr = it },
                        label = { Text("Resmi Tatil Çarpanı", color = TextGray) },
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Otomatik Mola Kesilsin", color = TextWhite, fontSize = 13.sp)
                        Switch(
                            checked = isBreakAutoDeduct,
                            onCheckedChange = { isBreakAutoDeduct = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
                        )
                    }

                    if (isBreakAutoDeduct) {
                        Divider(color = LightSlate, modifier = Modifier.padding(vertical = 4.dp))
                        
                        // Öğle Molası Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Öğle Molası Düşülsün", color = TextWhite, fontSize = 13.sp)
                                Switch(
                                    checked = isLunchBreakEnabled,
                                    onCheckedChange = { isLunchBreakEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
                                )
                            }
                            if (isLunchBreakEnabled) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = lunchStartStr,
                                        onValueChange = { lunchStartStr = it },
                                        label = { Text("Başlangıç", color = TextGray, fontSize = 11.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedBorderColor = EmeraldGreen,
                                            unfocusedBorderColor = LightSlate
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = lunchEndStr,
                                        onValueChange = { lunchEndStr = it },
                                        label = { Text("Bitiş", color = TextGray, fontSize = 11.sp) },
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
                        }

                        Divider(color = LightSlate, modifier = Modifier.padding(vertical = 4.dp))

                        // Çay Molası Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Çay / Kahve Molası Düşülsün", color = TextWhite, fontSize = 13.sp)
                                Switch(
                                    checked = isCoffeeBreakEnabled,
                                    onCheckedChange = { isCoffeeBreakEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
                                )
                            }
                            if (isCoffeeBreakEnabled) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = coffeeStartStr,
                                        onValueChange = { coffeeStartStr = it },
                                        label = { Text("Başlangıç", color = TextGray, fontSize = 11.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedBorderColor = EmeraldGreen,
                                            unfocusedBorderColor = LightSlate
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = coffeeEndStr,
                                        onValueChange = { coffeeEndStr = it },
                                        label = { Text("Bitiş", color = TextGray, fontSize = 11.sp) },
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
                        }

                        Divider(color = LightSlate, modifier = Modifier.padding(vertical = 4.dp))

                        OutlinedTextField(
                            value = extraBreakMinutesStr,
                            onValueChange = { extraBreakMinutesStr = it },
                            label = { Text("Ekstra Mola Süresi (Dakika)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = LightSlate
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val baseJob = editingJob ?: Job(name = jobName, employmentType = employmentType)
                        val updated = baseJob.copy(
                            name = jobName,
                            employmentType = employmentType,
                            monthlySalary = monthlySalaryStr.toDoubleOrNull() ?: 0.0,
                            dailyWage = dailyWageStr.toDoubleOrNull() ?: 0.0,
                            hourlyRate = hourlyRateStr.toDoubleOrNull() ?: 0.0,
                            standardStartTime = standardStartStr,
                            standardEndTime = standardEndStr,
                            overtimeMultiplier = overtimeMultStr.toDoubleOrNull() ?: 1.5,
                            nightShiftMultiplier = nightShiftMultStr.toDoubleOrNull() ?: 1.25,
                            holidayMultiplier = holidayMultStr.toDoubleOrNull() ?: 2.0,
                            isBreakAutoDeduct = isBreakAutoDeduct,
                            isLunchBreakEnabled = isLunchBreakEnabled,
                            isCoffeeBreakEnabled = isCoffeeBreakEnabled,
                            lunchBreakStart = lunchStartStr,
                            lunchBreakEnd = lunchEndStr,
                            coffeeBreakStart = coffeeStartStr,
                            coffeeBreakEnd = coffeeEndStr,
                            extraBreakMinutes = extraBreakMinutesStr.toIntOrNull() ?: 0
                        )

                        if (isEditing) {
                            onUpdateJob(updated)
                        } else {
                            onAddJob(updated)
                        }

                        showAddJobDialog = false
                        editingJob = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddJobDialog = false
                        editingJob = null
                    }
                ) {
                    Text("Vazgeç", color = TextGray)
                }
            }
        )
    }
}

// Simple data class representation for Ayarlar rows
data class SettingsItem(
    val label: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun generateTextReport(job: Job, breakdowns: List<DailyBreakdown>): String {
    val sb = java.lang.StringBuilder()
    sb.append("=========================================\n")
    sb.append("   ÇALIŞMA SAATLERİ & HAKEDİŞ RAPORU    \n")
    sb.append("=========================================\n")
    sb.append("İş Profili: ${job.name}\n")
    sb.append("Gelir Türü: ${job.employmentType}\n")
    sb.append("Rapor Tarihi: ${TimeEngine.formatDate(System.currentTimeMillis())}\n")
    sb.append("-----------------------------------------\n\n")

    sb.append(String.format("%-22s | %-8s | %-12s\n", "Tarih", "Net Saat", "Hakediş"))
    sb.append("-----------------------------------------\n")
    
    breakdowns.filter { it.netHours > 0 || it.isRestDay }.forEach { b ->
        sb.append(String.format("%-22s | %-8.1f | ₺%,.2f\n", b.dateString, b.netHours, b.earnings))
    }
    
    sb.append("-----------------------------------------\n")
    sb.append(String.format("Toplam Çalışılan Süre: %.1f saat\n", breakdowns.sumOf { it.netHours }))
    sb.append(String.format("Toplam Fazla Mesai: %.1f saat\n", breakdowns.sumOf { it.overtimeHours }))
    sb.append(String.format("Toplam Mola Süresi: %d dakika\n", breakdowns.sumOf { it.totalBreakMinutes }))
    sb.append(String.format("TOPLAM NET HAKEDİŞ: ₺%,.2f\n", breakdowns.sumOf { it.earnings }))
    sb.append("=========================================\n")
    return sb.toString()
}

private fun generateCsvReport(breakdowns: List<DailyBreakdown>): String {
    val sb = java.lang.StringBuilder()
    sb.append("Tarih,Net Calisilan Saat,Normal Saat,Fazla Mesai,Hafta Sonu Saat,Tatil Saat,Mola Dakika,Gunluk Hakedis (TL)\n")
    breakdowns.forEach { b ->
        sb.append(String.format(
            Locale.US,
            "%s,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%.2f\n",
            b.dateString, b.netHours, b.normalHours, b.overtimeHours, b.weekendHours, b.holidayHours, b.totalBreakMinutes, b.earnings
        ))
    }
    return sb.toString()
}

private fun shareReport(context: Context, content: String, fileName: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Maaş ve Çalışma Saatleri Raporu")
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, "Raporu Paylaş"))
}
