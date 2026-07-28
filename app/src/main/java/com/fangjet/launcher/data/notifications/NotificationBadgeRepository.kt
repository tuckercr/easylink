package com.fangjet.launcher.data.notifications

import android.content.ComponentName
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the set of packages that currently have a badge-worthy notification.
 *
 * Written by [NotificationBadgeService] (a NotificationListenerService), read
 * by the Home screen to draw Pixel-style dots on the My Apps row. Empties
 * itself when the listener disconnects so stale dots never linger.
 */
@Singleton
class NotificationBadgeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _badgedPackages = MutableStateFlow<Set<String>>(emptySet())
    val badgedPackages: StateFlow<Set<String>> = _badgedPackages.asStateFlow()

    /** True when the user has granted Notification access to the launcher. */
    fun isListenerPermissionGranted(): Boolean =
        NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)

    /** The component the system settings page needs to highlight. */
    fun listenerComponent(): ComponentName = ComponentName(context, NotificationBadgeService::class.java)

    internal fun update(active: Array<StatusBarNotification>?) {
        _badgedPackages.value = computeBadged(
            (active ?: emptyArray()).map { sbn ->
                NotificationInfo(
                    packageName = sbn.packageName,
                    isOngoing = sbn.isOngoing,
                    isGroupSummary = sbn.notification.flags and
                        android.app.Notification.FLAG_GROUP_SUMMARY != 0,
                )
            },
        )
    }

    internal fun clear() {
        _badgedPackages.value = emptySet()
    }

    companion object {
        /** Minimal shape of a notification for testable badge logic. */
        data class NotificationInfo(
            val packageName: String,
            val isOngoing: Boolean,
            val isGroupSummary: Boolean,
        )

        /**
         * Pixel-style dot rules: a package is badged when it has at least one
         * notification that is neither ongoing (music playback, navigation,
         * foreground services) nor a group summary (bookkeeping, not content).
         */
        fun computeBadged(notifications: List<NotificationInfo>): Set<String> =
            notifications
                .filter { !it.isOngoing && !it.isGroupSummary }
                .map { it.packageName }
                .toSet()
    }
}
