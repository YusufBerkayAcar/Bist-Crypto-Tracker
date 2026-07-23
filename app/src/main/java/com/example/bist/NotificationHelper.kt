package com.example.bist

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "bist_alarms_channel"
        private const val CHANNEL_NAME = "BIST Fiyat Alarmları"
        private const val CHANNEL_DESC = "Hisse senedi fiyat alarmları için bildirimler"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAlarmNotification(alarm: StockAlarm, currentValueText: String) {
        // Check for POST_NOTIFICATIONS permission on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val conditionText = when (alarm.type) {
            AlarmType.PRICE_ABOVE -> "belirlenen fiyatın üstüne çıktı"
            AlarmType.PRICE_BELOW -> "belirlenen fiyatın altına düştü"
            AlarmType.CHANGE_ABOVE -> "günlük artış limitini aştı"
            AlarmType.CHANGE_BELOW -> "günlük düşüş limitini aştı"
        }

        val comparisonSign = when (alarm.type) {
            AlarmType.PRICE_ABOVE -> ">="
            AlarmType.PRICE_BELOW -> "<="
            AlarmType.CHANGE_ABOVE -> ">="
            AlarmType.CHANGE_BELOW -> "<="
        }

        val targetSuffix = when (alarm.type) {
            AlarmType.PRICE_ABOVE, AlarmType.PRICE_BELOW -> " ₺"
            AlarmType.CHANGE_ABOVE, AlarmType.CHANGE_BELOW -> "%"
        }

        val contentTitle = "Alarm Tetiklendi: ${alarm.hisse}"
        val contentText = "${alarm.hisse} $conditionText! Şu an: $currentValueText (Hedef: $comparisonSign ${alarm.thresholdValue}$targetSuffix)"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = alarm.hisse.hashCode()
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission missing
        }
    }
}
