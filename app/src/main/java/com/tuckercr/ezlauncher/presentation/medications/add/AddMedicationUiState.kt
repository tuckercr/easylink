package com.tuckercr.ezlauncher.presentation.medications.add

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * UI state for the Add/Edit Medication screen.
 *
 * Rather than splitting into Loading/Success/Error sealed classes (the list
 * pattern), this screen uses a single form-state class because the data is
 * always present — the only "loading" moment is after the user taps Save.
 */
data class AddMedicationUiState(

    // ── Edit context (0L = new medication) ────────────────────────────────
    val medicationId: Long = 0L,
    val isLoadingExisting: Boolean = false,

    // ── Form fields ────────────────────────────────────────────────────────
    val name: String    = "",
    val dosage: String  = "",
    val notes: String   = "",

    /** Reminder times shown in the time-chip list. At least one required. */
    val reminderTimes: List<LocalTime> = emptyList(),

    /** Days of the week the medication should be taken. Defaults to every day. */
    val activeDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),

    val isActive: Boolean = true,

    // ── Validation errors (null = no error) ───────────────────────────────
    val nameError: String?          = null,
    val reminderTimesError: String? = null,
    val activeDaysError: String?    = null,

    // ── Save lifecycle ────────────────────────────────────────────────────
    val isSaving: Boolean  = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
) {
    val isEditing: Boolean get() = medicationId != 0L
}
