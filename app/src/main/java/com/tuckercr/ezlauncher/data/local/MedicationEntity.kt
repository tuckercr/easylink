package com.tuckercr.ezlauncher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tuckercr.ezlauncher.domain.model.Medication
import com.tuckercr.ezlauncher.domain.model.MedicationColor
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val notes: String,
    /** Stored as "HH:mm,HH:mm,…" — see [MedicationConverters]. */
    val reminderTimes: List<LocalTime>,
    /** Stored as "1,2,…" (DayOfWeek.value) — see [MedicationConverters]. */
    val activeDays: Set<DayOfWeek>,
    val isActive: Boolean,
    val color: MedicationColor,
) {
    fun toDomain() =
        Medication(
            id = id,
            name = name,
            dosage = dosage,
            notes = notes,
            reminderTimes = reminderTimes,
            activeDays = activeDays,
            isActive = isActive,
            color = color,
        )

    companion object {
        fun fromDomain(m: Medication) =
            MedicationEntity(
                id = m.id,
                name = m.name,
                dosage = m.dosage,
                notes = m.notes,
                reminderTimes = m.reminderTimes,
                activeDays = m.activeDays,
                isActive = m.isActive,
                color = m.color,
            )
    }
}
