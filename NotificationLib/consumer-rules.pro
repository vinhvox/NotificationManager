############################
# Parcelable / Parcelize
############################
# Giữ NotificationConfig và tất cả class con implement Parcelable
-keep class com.vio.notificationlib.domain.entities.NotificationConfig implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keep class com.vio.notificationlib.domain.entities.** implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Giữ CREATOR cho mọi Parcelable (dự phòng)
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Kotlin Parcelize plugin
-keep class kotlinx.parcelize.** { *; }
-keepclassmembers class ** implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

############################
# Gson / JSON (nếu bạn dùng Gson để serialize config)
############################
# Giữ các model để Gson parse chính xác
-keep class com.vio.notificationlib.domain.entities.** { *; }
-keepclassmembers class com.vio.notificationlib.domain.entities.** {
    <fields>;
}

############################
# Alarm / Notification
############################
# Giữ BroadcastReceiver, Service, Activity liên quan đến notificationlib
-keep class com.vio.notificationlib.presentation.NotificationReceiver { *; }
-keep class com.vio.notificationlib.data.datasource.AlarmNotificationScheduler { *; }
-keep class com.vio.notificationlib.presentation.NotificationManager { *; }

############################
# General safe rules
############################
# Giữ annotation (Parcelize, SerializedName, Keep…)
-keepattributes *Annotation*

# Giữ Enum names (nếu dùng trong config)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Gson specific classes
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }