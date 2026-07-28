package com.fangjet.launcher.data.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fires once daily via [HomeScreenCheckScheduler].
 *
 * If EasyLink Launcher is already the default home screen the pending notification
 * is cancelled (cleans up any previously shown reminder). Otherwise a
 * notification is posted directing the user to open the app and set it.
 */
@AndroidEntryPoint
class HomeScreenCheckReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: HomeScreenNotificationHelper

    @Inject
    lateinit var defaultHomeChecker: DefaultHomeChecker

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) = handleAlarm()

    // internal (not private) only so unit tests can call this directly, bypassing the
    // Hilt-generated onReceive wrapper. Not intended to be called from production code.
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    internal fun handleAlarm() {
        if (defaultHomeChecker.isDefault()) {
            notificationHelper.cancel()
        } else {
            notificationHelper.showNotification()
        }
    }
}
