package com.tuckercr.ezlauncher.domain.repository

import com.tuckercr.ezlauncher.domain.model.Medication
import com.tuckercr.ezlauncher.domain.model.ReminderAction
import com.tuckercr.ezlauncher.domain.model.ReminderLog
import com.tuckercr.ezlauncher.domain.model.TodayReminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Domain contract for all medication reminder data and scheduling.
 *
 * Intentionally flat — persistence (Room), scheduling (AlarmManager),
 * and notification logic all sit behind this single interface. The domain
 * layer declares what it needs; the data layer decides how to provide it.
 */
interface MedicationRepository {

    /** Live stream of all saved medications, ordered by name. */
    fun getMedications(): Flow<List<Medication>>

    /** One-shot fetch — used inside alarm receivers that can't collect a Flow. */
    suspend fun getMedicationById(id: Long): Medication?

    /**
     * Persist a new or updated medication, then reschedule all its alarms.
     * Returns the saved medication with its assigned [Medication.id].
     */
    suspend fun saveMedication(medication: Medication): Medication

    /**
     * Delete a medication and cancel all its scheduled alarms.
     */
    suspend fun deleteMedication(medication: Medication)

    /**
     * Build the chronological list of reminder slots for today.
     * Cross-references today's logs to determine status of each slot.
     */
    fun getTodayReminders(): Flow<List<TodayReminder>>

    /** Record the user's response to a reminder (TAKEN / SNOOZED / MISSED). */
    suspend fun logReminderAction(
        medicationId: Long,
        scheduledTime: LocalDateTime,
        action: ReminderAction,
    )

    /** Reminder history since [since], newest first. */
    fun getReminderHistory(since: LocalDateTime): Flow<List<ReminderLog>>

    /**
     * Re-schedule all active medication alarms.
     * Called on BOOT_COMPLETED — AlarmManager does not survive reboots.
     */
    suspend fun rescheduleAllAlarms()
}
