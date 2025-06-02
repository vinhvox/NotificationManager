package com.vio.notificationlib.presentation

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.vio.notificationlib.R
import com.vio.notificationlib.databinding.ActivityFullscreenNotificationBinding
import com.vio.notificationlib.domain.entities.NotificationConfig

class FullscreenNotificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFullscreenNotificationBinding.inflate(layoutInflater)
        Log.e("NotificationManager", "onCreate: FullscreenNotificationActivity")
        setContentView(binding.root)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("schedule_data", NotificationConfig::class.java)
        } else {
            intent.getParcelableExtra("schedule_data")
        }?.let { content ->
            binding.txtContentNoti.text = content.title
            binding.txtDescriptionNoti.text = content.body
            binding.txtOpenNow.text = content.cta
            binding.imgContent.loadWithFallback(
                url = content.imageUrl,
                placeholderRes = R.drawable.img_content_lock_screen
            )
            binding.imgBackground.loadWithFallback(
                url = content.backgroundUrl,
                placeholderRes = R.drawable.img_bg_lock_screen
            )
            binding.txtOpenNow.text = content.cta
           binding.txtOpenNow.setOnClickListener {
               val intent = Intent(this, content.activityClassName).apply {
                   flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                   putExtra("isFromLockScreen", true)
                   putExtra("target_feature", content.targetFeature)
               }
               startActivity(intent)
               finish()
           }
           binding.ctlPattenLock.setOnClickListener {
               val intent = Intent(this, content.activityClassName).apply {
                   flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                   putExtra("isFromLockScreen", true)
                   putExtra("target_feature", content.targetFeature)
               }
               startActivity(intent)
               finish()
           }
        }
        binding.txtClose.setOnClickListener {
            finish()
        }
    }

    fun ImageView.loadWithFallback(
        url: String,
        placeholderRes: Int,
        fallbackDomain: String = "https://photos.lordeaglesoftware.com/",
        fallbackBaseUrl: String = "http://64.176.221.209/"
    ) {
        val context = this.context
        val uri = url.trim().toUri()
        Glide.with(context).load(uri)
            .placeholder(placeholderRes)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    if (url.contains(fallbackDomain)) {
                        val fallbackUrl = url.replace(fallbackDomain, fallbackBaseUrl)
                        Log.d("GlideFallback", "Retrying with fallback URL: $fallbackUrl")

                        Handler(Looper.getMainLooper()).post {
                            Glide.with(context)
                                .load(fallbackUrl.toUri())
                                .placeholder(placeholderRes)
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .into(this@loadWithFallback)
                        }
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

            }).into(this)
    }
}