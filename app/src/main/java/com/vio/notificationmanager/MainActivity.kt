package com.vio.notificationmanager

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.vio.notificationlib.data.datasource.FirebaseRemoteConfigDataSource
import com.vio.notificationlib.data.datasource.WorkManagerScheduler
import com.vio.notificationlib.data.repository.NotificationRepositoryImpl
import com.vio.notificationlib.domain.entities.NotificationConfig
import com.vio.notificationlib.domain.usecases.FetchNotificationConfigUseCase
import com.vio.notificationlib.domain.usecases.ScheduleNotificationsUseCase
import com.vio.notificationlib.presentation.NotificationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val firebaseDataSource = FirebaseRemoteConfigDataSource(FirebaseRemoteConfig.getInstance(), this)
        val workManagerScheduler = WorkManagerScheduler(this)
        val repository = NotificationRepositoryImpl(firebaseDataSource, workManagerScheduler)
        val fetchUseCase = object : FetchNotificationConfigUseCase {
            override suspend fun execute() = repository.fetchNotificationConfigs()
        }
        val scheduleUseCase = object : ScheduleNotificationsUseCase{
            override suspend fun execute(configs: List<NotificationConfig>) {
                android.util.Log.e("TAG", "execute: ${configs.size}", )
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
            notificationManager.fetchAndScheduleNotifications()
        }
    }
}

