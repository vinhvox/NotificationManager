package com.vio.notificationlib.domain.usecases

import com.vio.notificationlib.domain.entities.NotificationConfig

interface FetchNotificationConfigUseCase {
    suspend fun execute(): List<NotificationConfig>
}