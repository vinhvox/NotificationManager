package com.vio.notificationlib.presentation

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.vio.notificationlib.domain.entities.NotificationConfig
import com.vio.notificationlib.domain.usecases.FetchNotificationConfigUseCase
import com.vio.notificationlib.domain.usecases.ScheduleNotificationsUseCase

class NotificationWorker(private val appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.failure()
        val title = inputData.getString("title") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()
        val targetFeature = inputData.getString("target_feature")
        val customLayout = inputData.getInt("custom_layout", -1)

        val notificationManager = NotificationManager(
            appContext,
            null,
            object : FetchNotificationConfigUseCase {
                override suspend fun execute(): List<NotificationConfig> = emptyList()
            },
            object : ScheduleNotificationsUseCase {
                override suspend fun execute(configs: List<NotificationConfig>) {}
            }
        )
        notificationManager.showNotification(
            id,
            title,
            body,
            targetFeature,
            if (customLayout != -1) customLayout else null
        )
        return Result.success()
    }
}