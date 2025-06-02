package com.vio.notificationlib.presentation

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vio.notificationlib.R
import com.vio.notificationlib.domain.entities.NotificationConfig

class FullscreenNotificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("NotificationManager", "onCreate: FullscreenNotificationActivity", )
        setContentView(R.layout.activity_fullscreen_notification)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val title = intent.getStringExtra("title") ?: "Notification"
        val body = intent.getStringExtra("body") ?: ""
        val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("schedule_data", NotificationConfig::class.java)
        } else {
            intent.getParcelableExtra("schedule_data")
        }

        findViewById<TextView>(R.id.notification_title).text = title
        findViewById<TextView>(R.id.notification_body).text = body
        findViewById<Button>(R.id.dismiss_button).setOnClickListener {
            finish()
        }

        // Xử lý targetFeature nếu cần
        config?.targetFeature?.let { feature ->
            // Ví dụ: Thực hiện hành động dựa trên targetFeature
        }
    }
}