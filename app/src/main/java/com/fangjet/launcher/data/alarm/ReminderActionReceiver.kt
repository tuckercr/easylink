package com.fangjet.launcher.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fangjet.launcher.data.alarm.ReminderNotificationHelper.Companion.ACTION_SNOOZE
import com.fangjet.launcher.data.alarm.ReminderNotificationHelper.Companion.ACTION_TAKEN
import com.fangjet.launcher.data.alarm.ReminderNotificationHelper.Companion.EXTRA_NOTIF_ID
import com.fangjet.launcher.data.alarm.ReminderNotificationHelper.Companion.SNOOZE_MINUTES
import com.fangjet.launcher.domain.model.ReminderAction
import com.fangjet.launcher.domain.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

private const val TAG = "ReminderActionReceiver"

/**
 * Handles the "TAKEN" and "SNOOZE 15 MIN" notification action buttons.
 *
 * This receiver fires in the background — the user can dismiss the
 * reminder without opening the app. Using [goAsync] ensures the Room
 * write and notification dismissal complete before the wake lock expires.
 *
 * ## TAKEN flow
 *  1. Log TAKEN in Room
 *  2. Dismiss the notification
 *
 * ## SNOOZE flow
 *  1. Log SNOOZED in Room
 *  2. Dismiss the notification
 *  3. Schedule a one-shot alarm for [SNOOZE_MINUTES] from now
 *     (which will show the notification again)
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MedicationRepository

    @Inject
    lateinit var notificationHelper: ReminderNotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val scheduledStr = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        val action = intent.action ?: return

        Log.d(TAG, "Action=$action for medicationId=$medicationId")

        scope.launch {
            try {
                val scheduledTime = LocalDateTime.parse(scheduledStr)

                when (action) {
                    ACTION_TAKEN -> {
                        repository.logReminderAction(
                            medicationId,
                            scheduledTime,
                            ReminderAction.TAKEN,
                        )
                        notificationHelper.dismissReminder(medicationId)
                        Log.d(TAG, "Logged TAKEN for med $medicationId")
                    }

                    ACTION_SNOOZE -> {
                        repository.logReminderAction(
                            medicationId,
                            scheduledTime,
                            ReminderAction.SNOOZED,
                        )
                        notificationHelper.dismissReminder(medicationId)
                        scheduleSnoozeAlarm(context, intent, medicationId)
                        Log.d(TAG, "Snoozed med $medicationId for $SNOOZE_MINUTES min")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Fire a one-shot alarm [SNOOZE_MINUTES] from now that re-shows the notification. */
    private fun scheduleSnoozeAlarm(
        context: Context,
        originalIntent: Intent,
        medicationId: Long,
    ) {
        val snoozeTime = LocalDateTime.now().plusMinutes(SNOOZE_MINUTES)
        val triggerMs = snoozeTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Reuse the alarm receiver but mark it as a snooze re-fire
        val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtras(originalIntent.extras ?: return)
            putExtra(EXTRA_SCHEDULED_TIME, snoozeTime.toString()) // update scheduled time
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (medicationId + 999_000).toInt(), // distinct from weekly alarm IDs
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            // Fallback for snooze: use inexact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
        }
    }
}
