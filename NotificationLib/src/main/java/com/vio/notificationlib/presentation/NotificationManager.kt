package com.vio.notificationlib.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vio.notificationlib.domain.entities.NotificationConfig
import java.text.SimpleDateFormat
import java.util.*

class NotificationManager(
    private val context: Context,
    private val activityClass: Class<*>?
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "custom_notification_channel"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    init {
        Log.d(TAG, "NotificationManager initialized")
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Custom Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for custom notifications"
            }
            notificationManager.createNotificationChannel(channel)
            val createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID, enabled=${createdChannel?.importance != NotificationManager.IMPORTANCE_NONE}")
        }
    }

    fun showNotification(config: NotificationConfig) {
        Log.d(TAG, "Notification triggered at ${dateFormat.format(Date())}: id=${config.id}, title=${config.title}, body=${config.body}, targetFeature=${config.targetFeature}, customLayout=${config.customLayout}")
        val notificationId = config.id
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(config.title)
            .setContentText(config.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        config.customLayout?.let {
            try {
                builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                builder.setCustomContentView(android.widget.RemoteViews(context.packageName, it))
                Log.d(TAG, "Applied custom layout: $it")
            } catch (e: Exception) {
                Log.e(TAG, "Error applying custom layout", e)
            }
        }

        activityClass?.let {
            try {
                val intent = Intent(context, it).apply {
                    config.targetFeature?.let { feature -> putExtra("target_feature", feature) }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(pendingIntent)
                Log.d(TAG, "Set PendingIntent for activity: ${it.simpleName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting PendingIntent", e)
            }
        }

        try {
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Notification sent: id=$notificationId at ${dateFormat.format(Date())}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing POST_NOTIFICATIONS permission or other security issue", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
        }
    }

    companion object {
        private const val TAG = "NotificationManager"
    }
}