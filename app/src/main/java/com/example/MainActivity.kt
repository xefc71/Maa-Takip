package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.GoldenGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WorkHoursViewModel

import android.os.Build
import com.example.util.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel: WorkHoursViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permissions for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        
        // Initialize notifications and schedule daily alarms
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.scheduleAllDailyAlarms(this)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val allJobs by viewModel.allJobs.collectAsStateWithLifecycle()
                val activeJob by viewModel.activeJob.collectAsStateWithLifecycle()
                val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
                val monthlyBreakdowns by viewModel.activeJobMonthlyBreakdowns.collectAsStateWithLifecycle()
                val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
                val liveTickerTime by viewModel.liveTicker.collectAsStateWithLifecycle(initialValue = System.currentTimeMillis())
                val currentSelectedDate by viewModel.currentSelectedDate.collectAsStateWithLifecycle()

                if (allJobs.isEmpty()) {
                    OnboardingScreen(
                        onComplete = { job ->
                            viewModel.addJob(job)
                        }
                    )
                } else {
                    var selectedTab by remember { mutableStateOf(0) }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = when (selectedTab) {
                                            0 -> "KAZANÇ & SÜRE"
                                            1 -> "MESAI GÜNLÜĞÜ"
                                            2 -> "ANALIZ & GRAFIK"
                                            else -> "PROFIL AYARLARI"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        color = GoldenGold,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.5.sp
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    label = { Text("Panel", fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Panel") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.testTag("nav_panel")
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    label = { Text("Günlükler", fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Günlükler") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.testTag("nav_logs")
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    label = { Text("Analizler", fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.Star, contentDescription = "Analizler") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.testTag("nav_analytics")
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    label = { Text("İşler", fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "İşler") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.testTag("nav_jobs")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            when (selectedTab) {
                                0 -> DashboardTab(
                                    activeJob = activeJob,
                                    activeSession = activeSession,
                                    monthlyBreakdowns = monthlyBreakdowns,
                                    liveTickerTime = liveTickerTime,
                                    onStartWork = { jobId -> viewModel.startWork(jobId) },
                                    onEndWork = { note, extraBreak, isHoliday, isWeekend ->
                                        viewModel.endWork(note, extraBreak, isHoliday, isWeekend)
                                    },
                                    allJobs = allJobs,
                                    onSelectJob = { id -> viewModel.selectJob(id) },
                                    onUpdateActiveSession = { session -> viewModel.updateManualSession(session) }
                                )

                                1 -> HistoryTab(
                                    activeJob = activeJob,
                                    sessions = allSessions,
                                    onAddSession = { session -> viewModel.addManualSession(session) },
                                    onUpdateSession = { session -> viewModel.updateManualSession(session) },
                                    onDeleteSession = { session -> viewModel.deleteSession(session) }
                                )

                                2 -> AnalyticsTab(
                                    activeJob = activeJob,
                                    monthlyBreakdowns = monthlyBreakdowns,
                                    currentSelectedDate = currentSelectedDate,
                                    onChangeMonth = { offset -> viewModel.changeMonth(offset) }
                                )

                                3 -> JobsTab(
                                    allJobs = allJobs,
                                    activeJob = activeJob,
                                    monthlyBreakdowns = monthlyBreakdowns,
                                    onAddJob = { job -> viewModel.addJob(job) },
                                    onUpdateJob = { job -> viewModel.updateJob(job) },
                                    onDeleteJob = { job -> viewModel.deleteJob(job) },
                                    onSelectJob = { id -> viewModel.selectJob(id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
