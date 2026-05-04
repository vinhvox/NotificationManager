package com.vio.notificationlib.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.vio.notificationlib.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

private val TAG = "loadBitmapWithFallback"

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

suspend fun loadBitmapWithFallback(
    context: Context,
    imageRes: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        Glide.with(context)
            .asBitmap()
            .load(imageRes)
            .placeholder(R.drawable.img_content_lock_screen)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .submit()
            .get()
    } catch (e: Exception) {
        null
    }
}

suspend fun loadBitmapWithFallback(
    context: Context,
    imageFile: File,
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        Glide.with(context)
            .asBitmap()
            .load(imageFile.path)
            .placeholder(R.drawable.img_content_lock_screen)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .submit()
            .get()
    } catch (e: Exception) {
        null
    }
}