package com.vio.notificationlib.data.datasource

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.vio.notificationlib.domain.entities.NotificationConfig
import com.vio.notificationlib.domain.entities.TimeConfig
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class FirebaseRemoteConfigDataSource(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    suspend fun fetchNotificationConfigs(): List<NotificationConfig> {
        try {
            val task = firebaseRemoteConfig.fetchAndActivate().await()
            Log.d(TAG, "fetchAndActivate result: $task")
            val configJson = firebaseRemoteConfig.getString("notification_config")

            if (configJson.isNotBlank()) {
                // Save to SharedPreferences if valid JSON
                editor.putString(KEY_NOTIFICATION_CONFIG, configJson).apply()
                Log.d(TAG, "Saved notification config to SharedPreferences")
                return parseJsonConfig(configJson)
            } else {
                Log.w(TAG, "Notification config from Firebase is empty, attempting to load from cache")
                // Try loading from SharedPreferences
                val cachedConfig = sharedPreferences.getString(KEY_NOTIFICATION_CONFIG, null)
                return if (cachedConfig != null) {
                    Log.d(TAG, "Loaded notification config from SharedPreferences")
                    parseJsonConfig(cachedConfig)
                } else {
                    Log.w(TAG, "No cached config available")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notification configs from Firebase", e)
            // Try loading from SharedPreferences on error
            val cachedConfig = sharedPreferences.getString(KEY_NOTIFICATION_CONFIG, null)
            return if (cachedConfig != null) {
                Log.d(TAG, "Loaded notification config from SharedPreferences due to Firebase error")
                parseJsonConfig(cachedConfig)
            } else {
                Log.w(TAG, "No cached config available")
                emptyList()
            }
        }
    }

    private fun parseJsonConfig(jsonString: String): List<NotificationConfig> {
        val configs = mutableListOf<NotificationConfig>()
        try {
            val jsonArray = JSONObject(jsonString).getJSONArray("notifications")
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val timeJson = json.getJSONObject("schedule_time")
                val days = if (json.has("days")) {
                    val daysArray = json.getJSONArray("days")
                    val daysList = mutableListOf<Int>()
                    for (j in 0 until daysArray.length()) {
                        daysList.add(daysArray.getInt(j))
                    }
                    daysList
                } else {
                    null
                }
                val config = NotificationConfig(
                    id = json.getString("id"),
                    title = json.getString("title"),
                    body = json.getString("body"),
                    scheduleType = json.getString("schedule_type"),
                    scheduleTime = TimeConfig(
                        hour = timeJson.getInt("hour"),
                        minute = timeJson.getInt("minute")
                    ),
                    days = days,
                    repeat = json.getBoolean("repeat"),
                    targetFeature = json.optString("target_feature", null),
                    customLayout = json.optInt("custom_layout", -1).takeIf { it != -1 }
                )
                configs.add(config)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON config", e)
        }
        return configs
    }

    companion object {
        private const val TAG = "FirebaseRemoteConfigDataSource"
        private const val PREF_NAME = "notification_module_prefs"
        private const val KEY_NOTIFICATION_CONFIG = "notification_config"
    }
}