package com.example.crickstats

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "STOP_ALARM" -> {
                val medicineName = intent.getStringExtra("medicineName") ?: return
                val originalTime = intent.getStringExtra("originalTime") ?: return

                // Stop current alarm
                (context.applicationContext as? MyApplication)?.stopCurrentAlarm()

                // Cancel both original and follow-up alarms
                cancelAlarms(context, medicineName, originalTime)

                // Remove notification
                NotificationManagerCompat.from(context).cancel(medicineName.hashCode())
            }
        }
    }

    private fun cancelAlarms(context: Context, medicineName: String, originalTime: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel original alarm
        val originalIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medicineName", medicineName)
            putExtra("originalTime", originalTime)
        }
        val originalPending = PendingIntent.getBroadcast(
            context,
            "$medicineName$originalTime".hashCode(),
            originalIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        originalPending?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        // Cancel follow-up alarm
        val followUpTime = getTimePlusOneMinute(originalTime)
        val followUpIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medicineName", medicineName)
            putExtra("originalTime", originalTime)
            putExtra("isFollowUp", true)
        }
        val followUpPending = PendingIntent.getBroadcast(
            context,
            "$medicineName$followUpTime".hashCode(),
            followUpIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        followUpPending?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun getTimePlusOneMinute(time: String): String {
        val (hour, minute) = time.split(":").map { it.toInt() }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            add(Calendar.MINUTE, 1)
        }
        return String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }
}
