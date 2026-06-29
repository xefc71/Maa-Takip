package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.model.WorkSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast
import android.content.BroadcastReceiver
import java.util.Calendar

object NotificationHelper {
    const val CHANNEL_ID = "work_hours_reminders"
    const val CHANNEL_NAME = "Mesai Hatırlatıcıları"

    const val ACTION_START_WORK = "com.example.ACTION_START_WORK"
    const val ACTION_END_WORK = "com.example.ACTION_END_WORK"
    const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = "Günlük mesai planı ve hatırlatmalar"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(context: Context, id: Int) {
        createNotificationChannel(context)

        val title: String
        val text: String
        var isStart = false
        var isEnd = false
        var hasSnooze = true

        when (id) {
            1001 -> {
                title = "🔔 İş Günün Başlıyor"
                text = "Günaydın! Vardiyanızın başlamasına 15 dakika kaldı. Hazır olduğunuzda çalışma takibini başlatın."
            }
            1002 -> {
                title = "⏰ Mesai Saati Geldi!"
                text = "Mesainizi başlatın. Yeni iş gününüz için başarılar dileriz!"
                isStart = true
            }
            1003 -> {
                title = "⚠️ Henüz Başlatılmadı"
                text = "Henüz çalışma başlatılmadı. Unutmadan mesainizi başlatın."
            }
            1004 -> {
                title = "🍽️ Öğle Arası"
                text = "Öğle molası otomatik olarak uygulanacak. Keyifli dinlenmeler!"
            }
            1005 -> {
                title = "⏳ Mesai Sonu Yaklaşıyor"
                text = "Normal mesainizin bitmesine 15 dakika kaldı. Hazırlıklarınızı tamamlayabilirsiniz."
            }
            1006 -> {
                title = "💼 Fazla Mesai Başlıyor"
                text = "Şu andan itibaren fazla mesai kazanmaya başlıyorsunuz. İyi çalışmalar!"
                isEnd = true
            }
            1007 -> {
                title = "🌙 Geç Saat Uyarısı"
                text = "Çalışmanız hâlâ aktif görünüyor. Bitirmeyi unutmayın. İyi geceler!"
            }
            else -> {
                title = "⏰ Mesai Hatırlatıcısı"
                text = "Çalışma durumunuzu kontrol etmeyi unutmayın."
            }
        }

        // Content intent to open MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            id,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Safe system icon fallback
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        // Actions
        if (isStart) {
            val startIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_START_WORK
                putExtra("notification_id", id)
            }
            val startPending = PendingIntent.getBroadcast(
                context,
                id + 10,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "🟢 Çalışmayı Başlat", startPending)
        }

        if (isEnd) {
            val endIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_END_WORK
                putExtra("notification_id", id)
            }
            val endPending = PendingIntent.getBroadcast(
                context,
                id + 20,
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "🔴 Çalışmayı Bitir", endPending)
        }

        if (hasSnooze) {
            val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra("notification_id", id)
            }
            val snoozePending = PendingIntent.getBroadcast(
                context,
                id + 30,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_lock_idle_alarm, "⏰ Ertele", snoozePending)
        }

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(id, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Schedules repeating alarms for the specific hours
    fun scheduleAllDailyAlarms(context: Context) {
        scheduleAlarm(context, 1001, 7, 45)
        scheduleAlarm(context, 1002, 8, 0)
        scheduleAlarm(context, 1003, 8, 10)
        scheduleAlarm(context, 1004, 12, 0)
        scheduleAlarm(context, 1005, 17, 45)
        scheduleAlarm(context, 1006, 18, 0)
        scheduleAlarm(context, 1007, 22, 0)
    }

    private fun scheduleAlarm(context: Context, id: Int, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmNotificationReceiver::class.java).apply {
            putExtra("notification_id", id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to regular set
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}

class AlarmNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("notification_id", 1001)
        NotificationHelper.showReminderNotification(context, id)
        
        // Re-schedule for next day
        NotificationHelper.scheduleAllDailyAlarms(context)
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("notification_id", 1001)
        val action = intent.action

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)

        val db = AppDatabase.getDatabase(context)

        when (action) {
            NotificationHelper.ACTION_START_WORK -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val activeJob = db.jobDao().getActiveJob()
                    if (activeJob != null) {
                        val activeSession = db.workSessionDao().getActiveSession()
                        if (activeSession == null) {
                            db.workSessionDao().insertSession(
                                WorkSession(
                                    jobId = activeJob.id,
                                    startTime = System.currentTimeMillis(),
                                    endTime = null
                                )
                            )
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "🟢 Çalışma başlatıldı!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "Zaten aktif bir çalışma var.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Lütfen önce aktif bir iş profili seçin.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            NotificationHelper.ACTION_END_WORK -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val activeSession = db.workSessionDao().getActiveSession()
                    if (activeSession != null) {
                        db.workSessionDao().updateSession(
                            activeSession.copy(
                                endTime = System.currentTimeMillis()
                            )
                        )
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "🔴 Çalışma bitirildi!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Aktif bir çalışma bulunamadı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            NotificationHelper.ACTION_SNOOZE -> {
                Toast.makeText(context, "⏰ Bildirim 15 dakika ertelendi.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
