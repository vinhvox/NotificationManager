package com.vio.notificationlib.presentation

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.vio.notificationlib.R
import com.vio.notificationlib.domain.entities.NotificationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationManager(
    private val context: Context,
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
                NotificationManager.IMPORTANCE_HIGH // Đặt mức độ quan trọng cao cho fullscreen
            ).apply {
                description = "Channel for custom notifications"
            }
            notificationManager.createNotificationChannel(channel)
            val createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            Log.d(
                TAG,
                "Notification channel created: $CHANNEL_ID, enabled=${createdChannel?.importance != NotificationManager.IMPORTANCE_NONE}"
            )
        }
    }

    fun showNotification(config: NotificationConfig) {
        Log.d(
            TAG,
            "Notification triggered at ${dateFormat.format(Date())}: id=${config.id}, title=${config.title}, body=${config.body}, targetFeature=${config.targetFeature}, customLayout=${config.customLayout}, notificationType=${config.notificationType}"
        )
        CoroutineScope(Dispatchers.Main).launch {
            when (config.notificationType) {
                "FULLSCREEN" -> createNotificationStandard(config, true)
                else -> createNotificationStandard(config, false)
            }
        }
    }

    suspend fun createNotificationStandard(config: NotificationConfig, isFullscreen: Boolean) {
        Log.d(TAG, "createNotificationStandard: isFullscreen=$isFullscreen")
        val notificationContent = config.title
        val notificationDescription = config.body
        val bitmap = if (!isFullscreen) loadBitmapWithFallback(context, config.imageUrl) else null
        // Create custom views
        val view = RemoteViews(context.packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.txtContentNoti, notificationContent)
            setOnClickPendingIntent(R.id.llNotiParent, getPendingIntentActivity(config))
            if (bitmap != null) setImageViewBitmap(R.id.img, bitmap)
            setViewVisibility(R.id.img, if (bitmap != null) View.VISIBLE else View.GONE)
        }

        val expandedView =
            RemoteViews(context.packageName, R.layout.notification_layout_expanded).apply {
                setTextViewText(R.id.txtContentNoti, notificationContent)
                setTextViewText(R.id.txtDescriptionNoti, notificationDescription)
                setOnClickPendingIntent(R.id.llNotiParent, getPendingIntentActivity(config))
                if (bitmap != null) setImageViewBitmap(R.id.img, bitmap)
                setViewVisibility(R.id.img, if (bitmap != null) View.VISIBLE else View.GONE)
            }

        val headerView =
            RemoteViews(context.packageName, R.layout.notification_layout_header).apply {
                setTextViewText(R.id.txtContentNoti, notificationContent)
                setTextViewText(R.id.txtDescriptionNoti, notificationDescription)
                setOnClickPendingIntent(R.id.llNotiParent, getPendingIntentActivity(config))
                if (bitmap != null) setImageViewBitmap(R.id.img, bitmap)
                setViewVisibility(R.id.img, if (bitmap != null) View.VISIBLE else View.GONE)
            }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(view)
            .setCustomBigContentView(expandedView)
            .setCustomHeadsUpContentView(headerView)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

        // Chỉ sử dụng fullScreenPendingIntent khi thiết bị khóa
        if (isFullscreen && isDeviceLocked(context)) {
            val fullScreenIntent =
                Intent(context, FullscreenNotificationActivity::class.java).apply {
                    putExtra("schedule_data", config)
                    putExtra("title", config.title)
                    putExtra("body", config.body)
                    config.targetFeature?.let { putExtra("target_feature", it) }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context, config.id, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
            Log.d(TAG, "Set fullscreen intent for notification id=${config.id}")
        } else {
            notificationBuilder.setContentIntent(getPendingIntentActivity(config))
        }

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing POST_NOTIFICATIONS permission")
            return
        }

        try {
            notificationManager.notify(config.id, notificationBuilder.build())
            Log.d(
                TAG,
                "Notification sent: id=${config.id}, isFullscreen=$isFullscreen at ${
                    dateFormat.format(Date())
                }"
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing permissions or other security issue", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
        }
    }


    private fun isDeviceLocked(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isKeyguardLocked
    }

    private fun getPendingIntentActivity(config: NotificationConfig): PendingIntent? {

        return config.getActivityClass().let {
            val intent = Intent(context, it).apply {
                config.targetFeature?.let { feature -> putExtra("target_feature", feature) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            PendingIntent.getActivity(
                context, config.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    companion object {
        private const val TAG = "NotificationManager"
    }

    suspend fun loadBitmapWithFallback(
        context: Context,
        url: String,
        fallbackDomain: String = "https://photos.lordeaglesoftware.com/",
        fallbackBaseUrl: String = "http://64.176.221.209/"
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            // thử load với URL gốc
            Glide.with(context)
                .asBitmap()
                .load(url)
                .placeholder(R.drawable.img_content_lock_screen)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .submit()
                .get()
        } catch (e: Exception) {
            Log.d(TAG, "loadBitmapWithFallback: ${e.message}")
            if (url.contains(fallbackDomain)) {
                val fallbackUrl = url.replace(fallbackDomain, fallbackBaseUrl)
                try {
                    Glide.with(context)
                        .asBitmap()
                        .load(fallbackUrl)
                        .placeholder(R.drawable.img_content_lock_screen)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .submit()
                        .get()
                } catch (e2: Exception) {
                    Log.d(TAG, "loadBitmapWithFallback: ${e2.message}")
                    null
                }
            } else null
        }
    }
}