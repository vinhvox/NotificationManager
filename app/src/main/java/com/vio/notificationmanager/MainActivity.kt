package com.vio.notificationmanager

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.vio.notificationlib.data.datasource.AlarmNotificationScheduler
import com.vio.notificationlib.data.datasource.FirebaseRemoteConfigDataSource
import com.vio.notificationlib.data.repository.NotificationRepositoryImpl
import com.vio.notificationlib.domain.entities.NotificationConfig
import com.vio.notificationlib.domain.usecases.FetchNotificationConfigUseCase
import com.vio.notificationlib.domain.usecases.ScheduleNotificationsUseCase
import com.vio.notificationlib.presentation.NotificationManager
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
        val firebaseDataSource = FirebaseRemoteConfigDataSource(FirebaseRemoteConfig.getInstance(), this)
        val scheduler = AlarmNotificationScheduler(this)
        val repository = NotificationRepositoryImpl(firebaseDataSource, scheduler)
        val fetchUseCase = object : FetchNotificationConfigUseCase {
            override suspend fun execute() = repository.fetchNotificationConfigs()
        }
        val scheduleUseCase = object : ScheduleNotificationsUseCase {
            override suspend fun execute(configs: List<NotificationConfig>) {
                configs.forEach {
                    Log.e(TAG, "execute: $it", )
                }
                repository.scheduleNotifications(configs)
            }
        }

        val notificationManager = NotificationManager(
            context = this,
            activityClass = MainActivity::class.java,
            fetchNotificationConfigUseCase = fetchUseCase,
            scheduleNotificationsUseCase = scheduleUseCase
        )

        lifecycleScope.launch {
            Log.d(TAG, "Starting notification scheduling")
            notificationManager.fetchAndScheduleNotifications()
            Log.d(TAG, "Completed notification scheduling")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

