package com.fangjet.launcher.data.debug

import android.content.Context
import com.fangjet.launcher.data.local.EmergencyContactDao
import com.fangjet.launcher.data.local.EmergencyContactEntity
import com.fangjet.launcher.data.local.MedicationDao
import com.fangjet.launcher.data.local.MedicationEntity
import com.fangjet.launcher.data.local.ReminderLogDao
import com.fangjet.launcher.data.local.ReminderLogEntity
import com.fangjet.launcher.data.local.SpeedDialDao
import com.fangjet.launcher.data.local.SpeedDialEntity
import com.fangjet.launcher.domain.model.MedicationColor
import com.fangjet.launcher.domain.model.ReminderAction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEBUG-ONLY sample data.
 *
 * Fills the database with realistic People, Medications, and emergency contacts
 * so the screens look populated for demos and pitch screenshots. Each table is
 * only seeded when it is currently empty, so this never clobbers real data.
 * Call site is guarded by `BuildConfig.DEBUG`, so it never runs in a release build.
 */
@Singleton
class DemoDataSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val speedDialDao: SpeedDialDao,
    private val emergencyContactDao: EmergencyContactDao,
    private val medicationDao: MedicationDao,
    private val reminderLogDao: ReminderLogDao,
) {
    suspend fun seedIfEmpty() {
        seedSpeedDial()
        seedEmergencyContacts()
        seedMedications()
    }

    private suspend fun seedSpeedDial() {
        if (speedDialDao.count() > 0) return
        listOf(
            DemoPerson("Sarah Chen", "5550101", "demo_avatar_sarah"),
            DemoPerson("Emma Chen", "5550102", "demo_avatar_emma"),
            DemoPerson("Dr. Alvarez", "5550103", "demo_avatar_alvarez"),
            DemoPerson("Robert Lee", "5550104", "demo_avatar_robert"),
        ).forEachIndexed { i, person ->
            speedDialDao.insert(
                SpeedDialEntity(
                    contactId = DEMO_CONTACT_ID_BASE + i,
                    name = person.name,
                    phoneNumber = person.phone,
                    // Illustrated demo avatar bundled in the debug source set —
                    // resolved by name because this class compiles against main.
                    photoUriString = "android.resource://${context.packageName}/drawable/${person.avatarRes}",
                    displayOrder = i,
                ),
            )
        }
    }

    private suspend fun seedEmergencyContacts() {
        if (emergencyContactDao.getAll().isNotEmpty()) return
        emergencyContactDao.upsert(
            EmergencyContactEntity(name = "Sarah Chen", phoneNumber = "5550101", isPrimary = true),
        )
        emergencyContactDao.upsert(
            EmergencyContactEntity(name = "Robert Lee", phoneNumber = "5550104", isPrimary = false),
        )
        emergencyContactDao.upsert(
            EmergencyContactEntity(name = "Dr. Alvarez", phoneNumber = "5550103", isPrimary = false),
        )
    }

    private suspend fun seedMedications() {
        if (medicationDao.observeAll().first().isNotEmpty()) return
        val allDays = DayOfWeek.entries.toSet()
        val today = LocalDate.now()
        val samples = listOf(
            Sample("Aspirin", "81 mg", LocalTime.of(8, 0), MedicationColor.GREEN, taken = true),
            Sample("Lisinopril", "10 mg", LocalTime.of(12, 0), MedicationColor.BLUE, taken = true),
            Sample("Vitamin D", "2000 IU", LocalTime.of(18, 0), MedicationColor.AMBER, taken = false),
        )
        samples.forEach { s ->
            val id = medicationDao.upsert(
                MedicationEntity(
                    name = s.name,
                    dosage = s.dosage,
                    notes = "",
                    reminderTimes = listOf(s.time),
                    activeDays = allDays,
                    isActive = true,
                    color = s.color,
                ),
            )
            if (s.taken) {
                val scheduled = LocalDateTime.of(today, s.time)
                reminderLogDao.upsert(
                    ReminderLogEntity(
                        medicationId = id,
                        medicationName = s.name,
                        dosage = s.dosage,
                        scheduledTime = scheduled,
                        actionTime = scheduled.plusMinutes(4),
                        action = ReminderAction.TAKEN,
                    ),
                )
            }
        }
    }

    private data class DemoPerson(
        val name: String,
        val phone: String,
        val avatarRes: String,
    )

    private data class Sample(
        val name: String,
        val dosage: String,
        val time: LocalTime,
        val color: MedicationColor,
        val taken: Boolean,
    )

    companion object {
        private const val DEMO_CONTACT_ID_BASE = 900_000L
    }
}
