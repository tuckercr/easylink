package com.tuckercr.ezlauncher.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tuckercr.ezlauncher.domain.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BootCompletedReceiver"

/**
 * Restores all medication reminder alarms after a device reboot.
 *
 * [AlarmManager] alarms do not survive device reboots. This receiver listens
 * for [Intent.ACTION_BOOT_COMPLETED] and calls
 * [MedicationRepository.rescheduleAllAlarms] to reinstate them.
 *
 * ## Manifest requirement
 * ```xml
 * <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 *
 * <receiver
 *     android:name=".data.alarm.BootCompletedReceiver"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.BOOT_COMPLETED" />
 *     </intent-filter>
 * </receiver>
 * ```
 *
 * ## Hilt injection
 * `@AndroidEntryPoint` enables Hilt injection in BroadcastReceivers.
 * The receiver lives only for the duration of [onReceive]; [goAsync] + a
 * coroutine scope keeps the wake lock alive while Room queries run.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: MedicationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "Boot completed — rescheduling medication alarms")
        val pendingResult = goAsync()

        scope.launch {
            try {
                repository.rescheduleAllAlarms()
                Log.i(TAG, "Alarms rescheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
