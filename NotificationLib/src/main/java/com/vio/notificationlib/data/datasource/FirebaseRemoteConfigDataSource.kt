package com.vio.notificationlib.data.datasource

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.vio.notificationlib.domain.entities.NotificationConfig
import com.vio.notificationlib.domain.entities.TimeConfig
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class FirebaseRemoteConfigDataSource(
    private val remoteConfig: FirebaseRemoteConfig,
    private val activityClass: Class<*>
) {
    suspend fun getNotificationConfigs(): List<NotificationConfig> {
        return try {
            remoteConfig.fetchAndActivate().await()
            val configJson = remoteConfig.getString("notification_configs")
            Log.d(TAG, "Firebase config JSON: $configJson")
            parseNotificationConfigs(configJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Firebase config", e)
            emptyList()
        }
    }

    private fun parseNotificationConfigs(json: String): List<NotificationConfig> {
        val configs = mutableListOf<NotificationConfig>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val id = jsonObject.getInt("id")
                val title = jsonObject.getString("title")
                val body = jsonObject.getString("body")
                val cta = jsonObject.getString("cta")
                val imageUrl = jsonObject.getString("imageUrl")
                val backgroundUrl = jsonObject.getString("backgroundUrl")
                val scheduleType = jsonObject.getString("schedule_type")
                val timeConfig = parseTimeConfig(jsonObject.getJSONObject("schedule_time"))
                val days = jsonObject.optJSONArray("days")?.let { array ->
                    (0 until array.length()).map { array.getInt(it) }
                }
                val repeat = jsonObject.optBoolean("repeat", false)
                val targetFeature = jsonObject.optString("target_feature", null)
                val customLayout = jsonObject.optInt("custom_layout", -1).takeIf { it != -1 }
                val notificationType = jsonObject.optString("notification_type", "STANDARD") // Thêm trường mới
                val repeatTimeMinutes = jsonObject.getInt("repeat_time_minutes")

                configs.add(
                    NotificationConfig(
                        id = id,
                        title = title,
                        body = body,
                        cta = cta,
                        imageUrl = imageUrl,
                        backgroundUrl = backgroundUrl,
                        scheduleType = scheduleType,
                        scheduleTime = timeConfig,
                        days = days,
                        repeat = repeat,
                        targetFeature = targetFeature,
                        customLayout = customLayout,
                        activityClassName = activityClass,
                        notificationType = notificationType,
                        repeatTimeMinutes = repeatTimeMinutes
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notification configs", e)
        }
        return configs
    }

    private fun parseTimeConfig(json: JSONObject): TimeConfig {
        return TimeConfig(
            hour = json.getInt("hour"),
            minute = json.getInt("minute")
        )
    }

    companion object {
        private const val TAG = "FirebaseRemoteConfigDataSource"
    }
}