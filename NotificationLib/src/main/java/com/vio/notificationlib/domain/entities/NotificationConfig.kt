package com.vio.notificationlib.domain.entities

import androidx.annotation.DrawableRes


data class NotificationConfig(
    val id: Int,
    val title: String,
    val body: String,
    val cta: String,
    val imageUrl: String,
    @param:DrawableRes val imageRes: Int? = null,
    val imageLocalPath: String? = null,
    val backgroundUrl: String,
    val scheduleType: String,
    val scheduleTime: TimeConfig? = null,
    val amount: String? = null,
    val days: List<Int>? = null,
    val repeat: Boolean = false,
    val targetFeature: String? = null,
    val customLayout: Int? = null,
    val activityClassName: String, // <-- String thay vì Class<*>
    val notificationType: String = "STANDARD" // STANDARD hoặc FULLSCREEN
) {
    fun getActivityClass(): Class<*>? {
        return try {
            Class.forName(activityClassName)
        } catch (e: ClassNotFoundException) {
            null
        }
    }
}