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
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "ReminderAlarmReceiver"

/**
 * Fires when an AlarmManager alarm triggers for a scheduled medication.
 *
 * ## BroadcastReceiver lifecycle
 * A receiver's [onReceive] runs on the main thread and must return quickly.
 * Any async work (Room queries, scheduling the next alarm) must use
 * [goAsync] to hold a wake lock while the coroutine completes — otherwise
 * Android may put the device back to sleep before the work finishes.
 *
 * ## Hilt in receivers
 * @[AndroidEntryPoint] enables Hilt injection in BroadcastReceivers.
 * The injected [MedicationRepository] and [ReminderNotificationHelper]
 * are from the singleton component — no new instances created here.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: ReminderNotificationHelper
    @Inject lateinit var repository: MedicationRepository

    // Scope survives the receiver's short lifecycle via goAsync()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()  // extend BroadcastReceiver window

        val medicationId   = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: return
        val dosage         = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
        val notes          = intent.getStringExtra(EXTRA_NOTES) ?: ""
        val scheduledStr   = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: return

        Log.d(TAG, "Alarm fired for $medicationName at $scheduledStr")

        scope.launch {
            try {
                val scheduledTime = LocalDateTime.parse(scheduledStr)

                // Show the notification
                notificationHelper.showReminder(
                    medicationId  = medicationId,
                    name          = medicationName,
                    dosage        = dosage,
                    notes         = notes,
                    scheduledTime = scheduledTime,
                )

                // Schedule the next occurrence (same medication, 7 days ahead)
                repository.getMedicationById(medicationId)?.let { medication ->
                    repository.saveMedication(medication)  // triggers reschedule
                }
            } finally {
                pendingResult.finish()  // release wake lock
            }
        }
    }
}
