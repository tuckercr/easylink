package com.fangjet.launcher.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fangjet.launcher.data.fall.FallDetectionManager
import com.fangjet.launcher.data.home.HomeScreenCheckScheduler
import com.fangjet.launcher.data.preferences.FallDetectionPreferences
import com.fangjet.launcher.domain.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BootCompletedReceiver"

/**
 * Restores persistent app services after a device reboot:
 *  1. Medication reminder alarms (AlarmManager alarms don't survive reboots)
 *  2. Fall detection foreground service (if the user had it enabled)
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MedicationRepository

    @Inject
    lateinit var fallPrefs: FallDetectionPreferences

    @Inject
    lateinit var fallManager: FallDetectionManager

    @Inject
    lateinit var homeScreenCheckScheduler: HomeScreenCheckScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "Boot completed — restoring services")
        val pendingResult = goAsync()

        scope.launch {
            try {
                // 1. Medication alarms
                repository.rescheduleAllAlarms()
                Log.i(TAG, "Medication alarms rescheduled")

                // 2. Fall detection
                if (fallPrefs.isEnabled.first()) {
                    fallManager.start()
                    Log.i(TAG, "Fall detection restarted")
                }

                // 3. Home screen reminder (AlarmManager alarms don't survive reboots)
                homeScreenCheckScheduler.schedule()
                Log.i(TAG, "Home screen reminder rescheduled")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring services after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
