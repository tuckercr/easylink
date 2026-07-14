package com.fangjet.launcher.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fangjet.launcher.domain.model.ReminderAction
import com.fangjet.launcher.domain.model.ReminderLog
import java.time.LocalDateTime

@Entity(
    tableName = "reminder_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            // SET_NULL so history survives medication deletion
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("medicationId"), Index("scheduledTime")],
)
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long?,
    val medicationName: String,
    val dosage: String,
    val scheduledTime: LocalDateTime,
    val actionTime: LocalDateTime?,
    val action: ReminderAction,
) {
    fun toDomain() =
        ReminderLog(
            id = id,
            medicationId = medicationId ?: -1L,
            medicationName = medicationName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            actionTime = actionTime,
            action = action,
        )

    companion object {
        fun fromDomain(log: ReminderLog) =
            ReminderLogEntity(
                id = log.id,
                medicationId = log.medicationId.takeIf { it > 0 },
                medicationName = log.medicationName,
                dosage = log.dosage,
                scheduledTime = log.scheduledTime,
                actionTime = log.actionTime,
                action = log.action,
            )
    }
}
