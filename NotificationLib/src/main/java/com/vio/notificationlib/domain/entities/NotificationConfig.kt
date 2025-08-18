package com.vio.notificationlib.domain.entities


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotificationConfig(
    val id: Int,
    val title: String,
    val body: String,
    val cta: String,
    val imageUrl: String,
    val backgroundUrl: String,
    val scheduleType: String,
    val scheduleTime: TimeConfig,
    val days: List<Int>? = null,
    val repeat: Boolean = false,
    val targetFeature: String? = null,
    val customLayout: Int? = null,
    val activityClassName: Class<*>,
    val notificationType: String = "STANDARD", // STANDARD hoặc FULLSCREEN
    val repeatTimeMinutes : Int = 0
) : Parcelable
