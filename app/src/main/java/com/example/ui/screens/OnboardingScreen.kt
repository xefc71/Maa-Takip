package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Job
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (Job) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    
    // Form States
    var jobName by remember { mutableStateOf("Birinci İş") }
    var selectedType by remember { mutableStateOf("MAASLI") } // "MAASLI", "YEVMİYELİ", "SAAT_ÜCRETLI"
    
    var monthlySalaryStr by remember { mutableStateOf("35000") }
    var dailyWageStr by remember { mutableStateOf("1500") }
    var hourlyRateStr by remember { mutableStateOf("200") }
    
    var standardStartStr by remember { mutableStateOf("08:00") }
    var standardEndStr by remember { mutableStateOf("18:00") }
    
    var selectedRestDays by remember { mutableStateOf(listOf("Pazar")) }
    
    var overtimeMultStr by remember { mutableStateOf("1.5") }
    var nightShiftMultStr by remember { mutableStateOf("1.25") }
    var holidayMultStr by remember { mutableStateOf("2.0") }
    
    var lunchStartStr by remember { mutableStateOf("12:00") }
    var lunchEndStr by remember { mutableStateOf("13:00") }
    var coffeeStartStr by remember { mutableStateOf("10:00") }
    var coffeeEndStr by remember { mutableStateOf("10:15") }
    var isBreakAutoDeduct by remember { mutableStateOf(true) }
    var extraBreakMinutesStr by remember { mutableStateOf("0") }

    val daysOfWeek = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Profil Kurulumu", 
                        color = GoldenGold, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CharcoalNavy
                )
            )
        },
        containerColor = CharcoalNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..4) {
                    val isActive = i <= step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isActive) EmeraldGreen else LightSlate)
                    )
                }
            }

            // Step Header
            Text(
                text = when (step) {
                    1 -> "Adım 1: İstihdam Türü"
                    2 -> "Adım 2: Gelir Bilgileri"
                    3 -> "Adım 3: Çalışma Saatleri"
                    else -> "Adım 4: Otomatik Mola Kuralları"
                },
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                when (step) {
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "İşinizi adlandırın ve maaş hesaplama türünüzü seçin.",
                                color = TextGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )
                            
                            OutlinedTextField(
                                value = jobName,
                                onValueChange = { jobName = it },
                                label = { Text("İş Adı (örn: Fabrika, Yazılım...)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = TextGray,
                                    focusedLabelColor = EmeraldGreen
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_job_name")
                            )

                            // Type selection cards
                            val types = listOf(
                                Triple("MAASLI", "Maaşlı Çalışan", "Aylık sabit maaş alırsınız. Pazar/Haftalık izin günleri ücretli izin olarak otomatik hesaba katılır."),
                                Triple("YEVMİYELİ", "Yevmiyeli Çalışan", "Günlük yevmiye alırsınız. Yalnızca çalıştığınız günler için ödeme hesaplanır."),
                                Triple("SAAT_ÜCRETLİ", "Saat Ücretli Çalışan", "Çalıştığınız her saat ve dakika hassas bir şekilde ücretlendirilir.")
                            )

                            types.forEach { (typeKey, title, desc) ->
                                val isSel = selectedType == typeKey
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) DeepGreen.copy(alpha = 0.4f) else DarkSlate)
                                        .border(
                                            width = 2.dp,
                                            color = if (isSel) EmeraldGreen else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedType = typeKey }
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSel,
                                            onClick = { selectedType = typeKey },
                                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(desc, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Maaş miktarınızı ve fazla mesai oranlarınızı tanımlayın.",
                                color = TextGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )

                            when (selectedType) {
                                "MAASLI" -> {
                                    OutlinedTextField(
                                        value = monthlySalaryStr,
                                        onValueChange = { monthlySalaryStr = it },
                                        label = { Text("Aylık Net Maaş (TL)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                "YEVMİYELİ" -> {
                                    OutlinedTextField(
                                        value = dailyWageStr,
                                        onValueChange = { dailyWageStr = it },
                                        label = { Text("Günlük Net Yevmiye (TL)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                else -> {
                                    OutlinedTextField(
                                        value = hourlyRateStr,
                                        onValueChange = { hourlyRateStr = it },
                                        label = { Text("Saatlik Ücret (TL)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Overtime Mults
                            OutlinedTextField(
                                value = overtimeMultStr,
                                onValueChange = { overtimeMultStr = it },
                                label = { Text("Normal Fazla Mesai Çarpanı (örn: 1.5)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = nightShiftMultStr,
                                onValueChange = { nightShiftMultStr = it },
                                label = { Text("Gece Vardiyası Ek Çarpanı (örn: 1.25)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = holidayMultStr,
                                onValueChange = { holidayMultStr = it },
                                label = { Text("Resmi Tatil Mesai Çarpanı (örn: 2.0)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    3 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Standart çalışma saatlerinizi ve haftalık izin gününüzü belirtin.",
                                color = TextGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = standardStartStr,
                                    onValueChange = { standardStartStr = it },
                                    label = { Text("Giriş (SS:DD)") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = standardEndStr,
                                    onValueChange = { standardEndStr = it },
                                    label = { Text("Çıkış (SS:DD)") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text("Haftalık İzin Günleri (Ücretli/Ödemesiz):", color = TextWhite, fontWeight = FontWeight.Bold)

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                daysOfWeek.forEach { day ->
                                    val isSelected = selectedRestDays.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) GoldenGold.copy(alpha = 0.3f) else LightSlate)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) GoldenGold else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedRestDays = if (isSelected) {
                                                    selectedRestDays.filter { it != day }
                                                } else {
                                                    selectedRestDays + day
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(day, color = if (isSelected) GoldenGold else TextWhite, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Önceden tanımlanmış mola aralıkları çalışma süresinden otomatik düşülecektir.",
                                color = TextGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Otomatik Mola Kesintisi Aktif", color = TextWhite, fontWeight = FontWeight.SemiBold)
                                Switch(
                                    checked = isBreakAutoDeduct,
                                    onCheckedChange = { isBreakAutoDeduct = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
                                )
                            }

                            if (isBreakAutoDeduct) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("Öğle Yemeği Molası", color = GoldenGold, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            OutlinedTextField(
                                                value = lunchStartStr,
                                                onValueChange = { lunchStartStr = it },
                                                label = { Text("Başlangıç") },
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = lunchEndStr,
                                                onValueChange = { lunchEndStr = it },
                                                label = { Text("Bitiş") },
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text("Çay/Kahve Molası", color = GoldenGold, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            OutlinedTextField(
                                                value = coffeeStartStr,
                                                onValueChange = { coffeeStartStr = it },
                                                label = { Text("Başlangıç") },
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = coffeeEndStr,
                                                onValueChange = { coffeeEndStr = it },
                                                label = { Text("Bitiş") },
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = extraBreakMinutesStr,
                                            onValueChange = { extraBreakMinutesStr = it },
                                            label = { Text("Ekstra Mola Süresi (Dakika)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    TextButton(
                        onClick = { step-- },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextGray)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Geri")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            val defaultWorkDays = daysOfWeek.filter { !selectedRestDays.contains(it) }.joinToString(",")
                            val defaultRestDays = selectedRestDays.joinToString(",")
                            
                            val job = Job(
                                name = jobName,
                                employmentType = selectedType,
                                monthlySalary = monthlySalaryStr.toDoubleOrNull() ?: 0.0,
                                dailyWage = dailyWageStr.toDoubleOrNull() ?: 0.0,
                                hourlyRate = hourlyRateStr.toDoubleOrNull() ?: 0.0,
                                standardStartTime = standardStartStr,
                                standardEndTime = standardEndStr,
                                weeklyWorkDays = defaultWorkDays,
                                weeklyRestDays = defaultRestDays,
                                overtimeMultiplier = overtimeMultStr.toDoubleOrNull() ?: 1.5,
                                nightShiftMultiplier = nightShiftMultStr.toDoubleOrNull() ?: 1.25,
                                holidayMultiplier = holidayMultStr.toDoubleOrNull() ?: 2.0,
                                lunchBreakStart = lunchStartStr,
                                lunchBreakEnd = lunchEndStr,
                                coffeeBreakStart = coffeeStartStr,
                                coffeeBreakEnd = coffeeEndStr,
                                isBreakAutoDeduct = isBreakAutoDeduct,
                                extraBreakMinutes = extraBreakMinutesStr.toIntOrNull() ?: 0,
                                isActive = true
                            )
                            onComplete(job)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Text(if (step == 4) "Kurulumu Tamamla" else "İleri", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (step == 4) Icons.Default.Check else Icons.Default.ArrowForward,
                        contentDescription = "Devam",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
