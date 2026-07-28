package com.fangjet.shared.model

/**
 * Everything a caregiver can configure remotely. Written by Care, read by the launcher.
 *
 * This mirrors what the launcher stores locally in Room/DataStore. The launcher
 * treats a newer [updatedAt] as authoritative and applies it on top of local
 * state, so a caregiver edit wins over a stale local copy without needing a
 * full three-way merge.
 */
data class ElderConfig(
    /** Ids of the home buttons to show, in display order. Matches `HomeButton.name`. */
    val homeButtons: List<String> = emptyList(),
    val contacts: List<ContactConfig> = emptyList(),
    val medications: List<MedicationConfig> = emptyList(),
    val emergencyContacts: List<ContactConfig> = emptyList(),
    /** Epoch millis of the last caregiver edit. Used for last-write-wins. */
    val updatedAt: Long = 0L,
    /** Uid of the caregiver who made the last edit, for the audit trail. */
    val updatedBy: String = "",
)

/**
 * A speed-dial or emergency contact.
 *
 * @property photoUrl Cloud Storage URL. The launcher caches it locally on first
 *   sync so the People screen still renders offline.
 */
data class ContactConfig(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val photoUrl: String? = null,
    val position: Int = 0,
)

/**
 * A medication schedule.
 *
 * @property timesOfDay Reminder times as minutes since midnight, so the value is
 *   timezone- and locale-independent on the wire. Both apps convert at the edge.
 * @property daysOfWeek ISO-8601 day numbers (1 = Monday … 7 = Sunday). Empty
 *   means every day.
 */
data class MedicationConfig(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val timesOfDay: List<Int> = emptyList(),
    val daysOfWeek: List<Int> = emptyList(),
    val colorKey: String = "",
    val active: Boolean = true,
)
