package com.fangjet.launcher.data.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Feeds [NotificationBadgeRepository] with the device's active notifications.
 *
 * The system binds this service only after the user grants Notification access
 * (Settings → Notifications → Device & app notifications). Until then it simply
 * never runs and the launcher shows no dots — the feature degrades to nothing
 * rather than to an error.
 */
@AndroidEntryPoint
class NotificationBadgeService : NotificationListenerService() {

    @Inject
    lateinit var repository: NotificationBadgeRepository

    override fun onListenerConnected() {
        Log.i(TAG, "Notification listener connected")
        repository.update(runCatching { activeNotifications }.getOrNull())
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "Notification listener disconnected")
        repository.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        repository.update(runCatching { activeNotifications }.getOrNull())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        repository.update(runCatching { activeNotifications }.getOrNull())
    }

    companion object {
        private const val TAG = "NotificationBadgeSvc"
    }
}
