package com.vio.notificationmanager

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.vio.notificationlib.utils.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissions ->
            val postNotificationsGranted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

            Log.d(
                TAG,
                "POST_NOTIFICATIONS granted: $postNotificationsGranted"
            )
            if (postNotificationsGranted) {
                scheduleNotifications()
            } else {
                Log.w(TAG, "Required permissions denied")
            }
        }

    private val notificationInitializer by lazy {
        NotificationHelper(this, MainActivity::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val permissions =android.Manifest.permission.POST_NOTIFICATIONS

        if ( checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(permissions)
            }
        } else {
            scheduleNotifications()
        }
    }

    private fun scheduleNotifications() {
        lifecycleScope.launch {
            notificationInitializer.scheduleNotifications()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

