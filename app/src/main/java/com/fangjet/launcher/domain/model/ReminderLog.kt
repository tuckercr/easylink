package com.fangjet.launcher.domain.model

import java.time.LocalDateTime

/** What the user did when a reminder fired. */
enum class ReminderAction { TAKEN, SNOOZED, MISSED }

/**
 * A single entry in the medication history log.
 *
 * Denormalising [medicationName] means the history screen still shows
 * the correct drug name even if the medication is later renamed or deleted.
 *
 * @param scheduledTime  When the reminder was supposed to fire.
 * @param actionTime     When the user actually responded (null = not yet responded).
 * @param action         What the user did — or MISSED if no response came.
 */
data class ReminderLog(
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val scheduledTime: LocalDateTime,
    val actionTime: LocalDateTime? = null,
    val action: ReminderAction,
)
