package com.vio.notificationlib.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.vio.notificationlib.data.datasource.AlarmNotificationScheduler
import com.vio.notificationlib.domain.entities.NotificationConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "NotificationReceiver started at ${dateFormat.format(Date())}")
        val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("config", NotificationConfig::class.java)
        } else {
            intent.getParcelableExtra("config")
        }
        val day = intent.getIntExtra("day", 0)
        val activityClassName = intent.getStringExtra("activity_class_name")

        if (config == null) {
            Log.e(TAG, "Missing notification config")
            return
        }

        Log.d(
            TAG,
            "Received config: id=${config.id}, title=${config.title}, body=${config.body}, day=$day, activityClassName=$activityClassName   ${config.activityClass}"
        )

        try {

            val notificationManager = NotificationManager(context, config.activityClass)
            notificationManager.showNotification(config)
            Log.d(TAG, "NotificationReceiver completed successfully for id=${config.id}")

            if (config.repeat) {
                val scheduler = AlarmNotificationScheduler(context)
                scheduler.setSingleSchedule(config)
                Log.d(TAG, "Rescheduled repeating notification: id=${config.id}, day=$day")
            }
        } catch (e: Exception) {
            Log.e(TAG, "NotificationReceiver failed for id=${config.id}", e)
        }
    }

    companion object {
        private const val TAG = "NotificationReceiver"
    }
}