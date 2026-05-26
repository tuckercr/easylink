package com.tuckercr.ezlauncher.data.repository

import com.tuckercr.ezlauncher.data.alarm.AlarmScheduler
import com.tuckercr.ezlauncher.data.local.MedicationDao
import com.tuckercr.ezlauncher.data.local.MedicationEntity
import com.tuckercr.ezlauncher.data.local.ReminderLogDao
import com.tuckercr.ezlauncher.data.local.ReminderLogEntity
import com.tuckercr.ezlauncher.domain.model.Medication
import com.tuckercr.ezlauncher.domain.model.ReminderAction
import com.tuckercr.ezlauncher.domain.model.ReminderLog
import com.tuckercr.ezlauncher.domain.model.TodayReminder
import com.tuckercr.ezlauncher.domain.model.TodayReminder.Status
import com.tuckercr.ezlauncher.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [MedicationRepository].
 *
 * ## Responsibilities
 *  - CRUD operations via [MedicationDao]
 *  - Reminder history via [ReminderLogDao]
 *  - Alarm lifecycle via [AlarmScheduler] (cancel then reschedule on save; cancel on delete)
 *  - Today's reminder list — cross-references scheduled times with today's logs
 *
 * ## getTodayReminders() design
 * We emit a new list whenever either medications or today's logs change (combine).
 * For each active medication × active day that matches today × reminder time, we
 * create a [TodayReminder] and assign its [Status] by looking up the log entry:
 *  - Log exists + action = TAKEN  → TAKEN
 *  - Log exists + action = SNOOZED → SNOOZED
 *  - No log + time in future      → UPCOMING
 *  - No log + time in past        → OVERDUE (never acknowledged)
 *  - Log exists + action = MISSED → MISSED
 */
@Singleton
class MedicationRepositoryImpl @Inject constructor(
    private val medicationDao: MedicationDao,
    private val reminderLogDao: ReminderLogDao,
    private val alarmScheduler: AlarmScheduler,
) : MedicationRepository {

    // ── Read ──────────────────────────────────────────────────────────────────

    override fun getMedications(): Flow<List<Medication>> = medicationDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getMedicationById(id: Long): Medication? = medicationDao.getById(id)?.toDomain()

    /**
     * Returns a reactive list of today's dose slots with their current status.
     *
     * Combines the full medication list with today's reminder logs so that
     * tapping "TAKEN" on a notification causes the UI to update immediately
     * without any manual refresh.
     */
    override fun getTodayReminders(): Flow<List<TodayReminder>> {
        val todayStart = LocalDate.now().atStartOfDay()
        val todayEnd = todayStart.plusDays(1)

        val medicationsFlow = medicationDao
            .getActive()
            .map { entities -> entities.map { it.toDomain() } }

        val logsFlow = reminderLogDao.getLogsForDay(
            dayStart = todayStart,
            dayEnd = todayEnd,
        )

        return combine(medicationsFlow, logsFlow) { medications, logs ->
            val now = LocalDateTime.now()
            val today = LocalDate.now().dayOfWeek

            // Build a lookup: (medicationId, scheduledTime) → ReminderLog
            // Convert to domain first so medicationId is non-nullable Long.
            val logsByKey = logs
                .map { it.toDomain() }
                .associateBy { Pair(it.medicationId, it.scheduledTime) }

            val reminders = mutableListOf<TodayReminder>()

            for (medication in medications) {
                if (today !in medication.activeDays) continue

                for (reminderTime in medication.reminderTimes) {
                    val scheduledAt = LocalDateTime.of(LocalDate.now(), reminderTime)
                    val log = logsByKey[Pair(medication.id, scheduledAt)]

                    val status = when {
                        log == null && scheduledAt.isAfter(now) -> Status.UPCOMING
                        log == null && !scheduledAt.isAfter(now) -> Status.OVERDUE
                        log?.action == ReminderAction.TAKEN -> Status.TAKEN
                        log?.action == ReminderAction.SNOOZED -> Status.SNOOZED
                        log?.action == ReminderAction.MISSED -> Status.MISSED
                        else -> Status.UPCOMING
                    }

                    reminders += TodayReminder(
                        medication = medication,
                        scheduledAt = scheduledAt,
                        status = status,
                    )
                }
            }

            reminders.sorted()
        }
    }

    override fun getReminderHistory(since: LocalDateTime): Flow<List<ReminderLog>> =
        reminderLogDao.observeHistory(since).map { entities -> entities.map { it.toDomain() } }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Persist a medication and refresh its alarms.
     *
     * Cancel first to clear any alarms that no longer apply (e.g. a reminder
     * time was deleted during edit), then schedule afresh.
     */
    override suspend fun saveMedication(medication: Medication): Medication {
        alarmScheduler.cancelForMedication(medication)
        val entity = MedicationEntity.fromDomain(medication)
        val id = medicationDao.upsert(entity)

        // If this is a new insert Room returns the rowId; update the model's id
        val saved = if (medication.id == 0L) medication.copy(id = id) else medication
        alarmScheduler.scheduleForMedication(saved)
        return saved
    }

    override suspend fun deleteMedication(medication: Medication) {
        alarmScheduler.cancelForMedication(medication)
        medicationDao.delete(MedicationEntity.fromDomain(medication))
    }

    override suspend fun logReminderAction(
        medicationId: Long,
        scheduledTime: LocalDateTime,
        action: ReminderAction,
    ) {
        val medication = medicationDao.getById(medicationId)
        val entity = ReminderLogEntity(
            medicationId = medicationId,
            medicationName = medication?.name ?: "Unknown",
            dosage = medication?.dosage ?: "",
            scheduledTime = scheduledTime,
            actionTime = LocalDateTime.now(),
            action = action,
        )
        reminderLogDao.upsert(entity)
    }

    /**
     * Re-schedule alarms for all active medications.
     * Called by [BootCompletedReceiver] after device reboot
     * (AlarmManager alarms do not survive reboots).
     */
    override suspend fun rescheduleAllAlarms() {
        val entities = medicationDao.getActive().first()
        entities.forEach { entity ->
            val medication = entity.toDomain()
            alarmScheduler.cancelForMedication(medication)
            alarmScheduler.scheduleForMedication(medication)
        }
    }
}
