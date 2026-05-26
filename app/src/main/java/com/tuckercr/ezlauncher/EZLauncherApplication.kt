package com.tuckercr.ezlauncher

import android.app.Application
import com.tuckercr.ezlauncher.data.alarm.ReminderNotificationHelper
import com.tuckercr.ezlauncher.data.fall.FallDetectionManager
import com.tuckercr.ezlauncher.data.fall.FallDetectionNotificationHelper
import com.tuckercr.ezlauncher.data.preferences.FallDetectionPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EZLauncherApplication : Application() {

    @Inject lateinit var reminderNotifHelper: ReminderNotificationHelper
    @Inject lateinit var fallNotifHelper:     FallDetectionNotificationHelper
    @Inject lateinit var fallPrefs:           FallDetectionPreferences
    @Inject lateinit var fallManager:         FallDetectionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        reminderNotifHelper.createChannel()
        fallNotifHelper.createChannels()
        restoreFallDetectionIfEnabled()
    }

    /**
     * If the user had fall detection turned on before the process was killed
     * (e.g. low-memory eviction), restart the service automatically.
     */
    private fun restoreFallDetectionIfEnabled() {
        scope.launch {
            if (fallPrefs.isEnabled.first()) {
                fallManager.start()
            }
        }
    }
}
