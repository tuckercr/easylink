package com.tuckercr.ezlauncher.data.home

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.presentation.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

const val CHANNEL_ID_HOME_REMINDER = "home_screen_reminder"
const val NOTIF_ID_HOME_REMINDER = 8_001

@Singleton
class HomeScreenNotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID_HOME_REMINDER,
            context.getString(R.string.notif_channel_home_reminder_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_home_reminder_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun showNotification() {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_HOME_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat
            .Builder(context, CHANNEL_ID_HOME_REMINDER)
            .setSmallIcon(R.drawable.ic_home)
            .setContentTitle(context.getString(R.string.notif_home_reminder_title))
            .setContentText(context.getString(R.string.notif_home_reminder_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIF_ID_HOME_REMINDER, notification)
    }

    fun cancel() {
        manager.cancel(NOTIF_ID_HOME_REMINDER)
    }

    private fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
}
