package com.vio.notificationlib.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vio.notificationlib.domain.usecases.FetchNotificationConfigUseCase
import com.vio.notificationlib.domain.usecases.ScheduleNotificationsUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationManager(
    private val context: Context,
    private val activityClass: Class<*>?,
    private val fetchNotificationConfigUseCase: FetchNotificationConfigUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "custom_notification_channel"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        Log.e(TAG, "createNotificationChannel: frr", )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Custom Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for custom notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun fetchAndScheduleNotifications() {
        val configs = fetchNotificationConfigUseCase.execute()
        scheduleNotificationsUseCase.execute(configs)
    }

    fun showNotification(id: String, title: String, body: String, targetFeature: String?, customLayout: Int?) {
        Log.e(TAG, "showNotification: ", )
        val notificationId = id.hashCode()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        customLayout?.let {
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            builder.setCustomContentView(android.widget.RemoteViews(context.packageName, it))
        }

        activityClass?.let {
            val intent = Intent(context, it).apply {
                targetFeature?.let { feature -> putExtra("target_feature", feature) }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
        }

        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "Notification '$id' ($title) triggered at ${dateFormat.format(Date())}")
    }

    companion object {
        private const val TAG = "NotificationManager"
    }
}
