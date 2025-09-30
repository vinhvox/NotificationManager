package com.vio.notificationlib.utils

import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

// Convert object to JSON
inline fun <reified T> T.toJson(): String {
    return try {
        Gson().toJson(this)
    } catch (e: Exception) {
        Log.e("TAG", "Error converting object to JSON", e)
        ""
    }
}

// Convert JSON to object
inline fun <reified T> String.fromJson(): T? {
    return try {
        Gson().fromJson(this, T::class.java)
    } catch (e: JsonSyntaxException) {
        Log.e("TAG", "Invalid JSON format: $this", e)
        null
    } catch (e: Exception) {
        Log.e("TAG", "Error parsing JSON: $this", e)
        null
    }
}

inline fun <reified T> Intent.putJsonExtra(key: String, value: T) {
    try {
        val json = Gson().toJson(value)
        putExtra(key, json)
    } catch (e: Exception) {
        Log.e("INTENT_TAG", "Error putting JSON extra for key=$key", e)
    }
}

// Get object from Intent
inline fun <reified T> Intent.getJsonExtra(key: String): T? {
    return try {
        val json = getStringExtra(key) ?: return null
        Gson().fromJson(json, T::class.java)
    } catch (e: JsonSyntaxException) {
        Log.e("INTENT_TAG", "Invalid JSON format for key=$key", e)
        null
    } catch (e: Exception) {
        Log.e("INTENT_TAG", "Error getting JSON extra for key=$key", e)
        null
    }
}