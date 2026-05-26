package com.tuckercr.ezlauncher.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Domain model for a single medication.
 *
 * Pure Kotlin — zero Room or Android imports.
 * Uses java.time types (available since API 26, our minSdk).
 *
 * @param reminderTimes  Ordered list of times within each [activeDays] day
 *                       when the user should be reminded. Typically 1–4 entries.
 * @param activeDays     Which days of the week reminders fire. Defaults to every day.
 * @param color          An int from [MedicationColor] used to colour the tile
 *                       so users can quickly distinguish medications at a glance.
 */
data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String, // e.g. "1 tablet", "500mg"
    val notes: String = "", // e.g. "Take with food"
    val reminderTimes: List<LocalTime>,
    val activeDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val isActive: Boolean = true,
    val color: MedicationColor = MedicationColor.BLUE,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && reminderTimes.isNotEmpty()
}

/** Colour palette for medication tiles — limited set keeps the UI clean. */
enum class MedicationColor {
    BLUE,
    GREEN,
    AMBER,
    CORAL,
    PURPLE,
}
