package com.tuckercr.ezlauncher.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.tuckercr.ezlauncher.domain.model.Medication
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AlarmScheduler"

// Intent extras — shared constants used by receivers
const val EXTRA_MEDICATION_ID = "medication_id"
const val EXTRA_MEDICATION_NAME = "medication_name"
const val EXTRA_DOSAGE = "dosage"
const val EXTRA_NOTES = "notes"
const val EXTRA_SCHEDULED_TIME = "scheduled_time" // ISO-8601 LocalDateTime string
const val EXTRA_ALARM_REQUEST = "alarm_request" // unique Int for cancellation

/**
 * Wraps [AlarmManager] to schedule and cancel exact medication reminders.
 *
 * ## Alarm ID strategy
 * Each alarm needs a unique Int request code for [PendingIntent].
 * Formula: `medicationId * 10_000 + reminderTimeIndex * 10 + dayOfWeek.value`
 * This supports up to 1,000 medications with up to 1,000 reminder times each.
 *
 * ## Why AlarmManager and not WorkManager?
 * WorkManager is for deferrable background work. Medication reminders are
 * time-critical — "take Metformin after breakfast at 8:00am" must fire at
 * 8:00am, not "some time around 8am when the OS gets around to it".
 * [AlarmManager.setExactAndAllowWhileIdle] fires even in Doze mode.
 *
 * ## Boot survival
 * AlarmManager alarms are cleared on device reboot. [BootCompletedReceiver]
 * calls [MedicationRepository.rescheduleAllAlarms] to restore them.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedule all future alarms for [medication] for the next 7 days.
     *
     * Scheduling only 7 days ahead avoids accumulating thousands of pending
     * intents. The last alarm of each week re-schedules the next 7 days
     * (handled via a repeating weekly pass in the receiver).
     *
     * In practice, for a launcher app the device is almost always on,
     * and [BootCompletedReceiver] re-schedules after reboots.
     */
    fun scheduleForMedication(medication: Medication) {
        if (!medication.isActive) return

        val now = LocalDateTime.now()
        val sevenDaysLater = now.plusDays(7)

        medication.reminderTimes.forEachIndexed { timeIndex, reminderTime ->
            medication.activeDays.forEach { day ->
                // Find the next occurrence of this day/time
                var candidate = nextOccurrence(day, reminderTime)
                while (candidate.isBefore(sevenDaysLater)) {
                    if (candidate.isAfter(now)) {
                        val requestCode = alarmRequestCode(medication.id, timeIndex, day)
                        scheduleAlarm(
                            requestCode = requestCode,
                            fireAt = candidate,
                            medication = medication,
                            scheduledTime = candidate,
                        )
                    }
                    candidate = candidate.plusWeeks(1)
                }
            }
        }
    }

    /**
     * Cancel all alarms for [medication].
     * Called before rescheduling (on edit) or on delete.
     */
    fun cancelForMedication(medication: Medication) {
        medication.reminderTimes.forEachIndexed { timeIndex, _ ->
            medication.activeDays.forEach { day ->
                val requestCode = alarmRequestCode(medication.id, timeIndex, day)
                val intent = alarmIntent(requestCode)
                alarmManager.cancel(intent)
                intent.cancel()
                Log.d(TAG, "Cancelled alarm $requestCode for ${medication.name}")
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun scheduleAlarm(
        requestCode: Int,
        fireAt: LocalDateTime,
        medication: Medication,
        scheduledTime: LocalDateTime,
    ) {
        val triggerMs = fireAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val pendingIntent = alarmIntent(requestCode, medication, scheduledTime)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            // Fallback: use inexact alarm — still fires but may be delayed slightly
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            Log.w(TAG, "SCHEDULE_EXACT_ALARM not granted — using inexact alarm")
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMs,
                pendingIntent,
            )
        }

        Log.d(TAG, "Scheduled alarm $requestCode for ${medication.name} at $fireAt")
    }

    private fun alarmIntent(
        requestCode: Int,
        medication: Medication? = null,
        scheduledTime: LocalDateTime? = null,
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_REQUEST, requestCode)
            medication?.let {
                putExtra(EXTRA_MEDICATION_ID, it.id)
                putExtra(EXTRA_MEDICATION_NAME, it.name)
                putExtra(EXTRA_DOSAGE, it.dosage)
                putExtra(EXTRA_NOTES, it.notes)
            }
            scheduledTime?.let {
                putExtra(EXTRA_SCHEDULED_TIME, it.toString())
            }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextOccurrence(
        day: DayOfWeek,
        time: LocalTime,
    ): LocalDateTime {
        val today = LocalDate.now()
        var date = today
        // Advance to the correct day of week
        while (date.dayOfWeek != day) date = date.plusDays(1)
        val candidate = LocalDateTime.of(date, time)
        // If that time has already passed today, go to next week
        return if (candidate.isAfter(LocalDateTime.now())) {
            candidate
        } else {
            candidate.plusWeeks(1)
        }
    }

    private fun alarmRequestCode(
        medicationId: Long,
        timeIndex: Int,
        day: DayOfWeek,
    ): Int = (medicationId * 10_000 + timeIndex * 10 + day.value).toInt()
}
