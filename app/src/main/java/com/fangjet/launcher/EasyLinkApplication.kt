package com.fangjet.launcher

import android.app.Application
import com.fangjet.launcher.data.alarm.ReminderNotificationHelper
import com.fangjet.launcher.data.config.SettingsDefaultsProvider
import com.fangjet.launcher.data.debug.DemoDataSeeder
import com.fangjet.launcher.data.fall.FallDetectionManager
import com.fangjet.launcher.data.fall.FallDetectionNotificationHelper
import com.fangjet.launcher.data.home.HomeScreenCheckScheduler
import com.fangjet.launcher.data.home.HomeScreenNotificationHelper
import com.fangjet.launcher.data.pairing.ConfigSyncManager
import com.fangjet.launcher.data.preferences.FallDetectionPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EasyLinkApplication : Application() {

    @Inject
    lateinit var reminderNotifHelper: ReminderNotificationHelper

    @Inject
    lateinit var fallNotifHelper: FallDetectionNotificationHelper

    @Inject
    lateinit var fallPrefs: FallDetectionPreferences

    @Inject
    lateinit var fallManager: FallDetectionManager

    @Inject
    lateinit var homeScreenNotifHelper: HomeScreenNotificationHelper

    @Inject
    lateinit var homeScreenCheckScheduler: HomeScreenCheckScheduler

    @Inject
    lateinit var demoDataSeeder: DemoDataSeeder

    @Inject
    lateinit var settingsDefaults: SettingsDefaultsProvider

    @Inject
    lateinit var configSyncManager: ConfigSyncManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        reminderNotifHelper.createChannel()
        homeScreenNotifHelper.createChannel()
        homeScreenCheckScheduler.schedule()
        if (BuildConfig.SAFETY_FEATURES) {
            // Safety flavor only. The standard (Play) build must not register
            // "Fall Detection" channels — they show up in the system's
            // notification settings for a feature the app doesn't have — and
            // its manifest has no FallDetectionService to restart (a restored
            // safety-flavor preference would crash the start attempt).
            fallNotifHelper.createChannels()
            restoreFallDetectionIfEnabled()
        }

        // Pull the latest server-tuned setting defaults. A no-op until Firebase is
        // wired; never blocks startup and swallows its own failures.
        scope.launch { settingsDefaults.refresh() }

        // Apply caregiver configuration from EasyLink Care. Inert until paired.
        configSyncManager.start(scope)

        // Debug builds only: populate empty tables with sample data for demos/screenshots.
        if (BuildConfig.DEBUG) {
            scope.launch { demoDataSeeder.seedIfEmpty() }
        }
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
