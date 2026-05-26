package com.tuckercr.ezlauncher.data.fall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.presentation.fall.FallAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

const val CHANNEL_ID_FALL_DETECTION = "fall_detection_service"
const val CHANNEL_ID_FALL_ALERT = "fall_alert"

const val NOTIF_ID_FALL_SERVICE = 9_001
const val NOTIF_ID_FALL_ALERT = 9_002

@Singleton
class FallDetectionNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager =
        context.getSystemService(NotificationManager::class.java)

    /** Create both notification channels (idempotent). */
    fun createChannels() {
        // Persistent service notification — silent, low visibility
        val serviceChannel = NotificationChannel(
            CHANNEL_ID_FALL_DETECTION,
            "Fall Detection",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shown while fall detection is running"
            setShowBadge(false)
        }

        // Full-screen fall alert — max priority, wakes screen
        val alertChannel = NotificationChannel(
            CHANNEL_ID_FALL_ALERT,
            "Fall Alert",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Fires when a fall is detected"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 100, 300, 100, 600)
            enableLights(true)
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alertChannel)
    }

    /** Persistent low-profile notification shown while the service is running. */
    fun buildServiceNotification(): Notification =
        NotificationCompat
            .Builder(context, CHANNEL_ID_FALL_DETECTION)
            .setSmallIcon(R.drawable.ic_home)
            .setContentTitle("Fall detection is active")
            .setContentText("Monitoring for falls in the background")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()

    /** High-priority alert notification with a full-screen intent. */
    fun showFallAlert() {
        val intent = Intent(context, FallAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat
            .Builder(context, CHANNEL_ID_FALL_ALERT)
            .setSmallIcon(R.drawable.ic_home)
            .setContentTitle("⚠️ Fall Detected!")
            .setContentText("Are you OK? Tap to respond.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        manager.notify(NOTIF_ID_FALL_ALERT, notification)
    }

    fun dismissFallAlert() {
        manager.cancel(NOTIF_ID_FALL_ALERT)
    }
}
