package com.fangjet.shared.model

/**
 * Something that happened on the elder's phone and that a caregiver should know about.
 *
 * Append-only: the launcher creates these, and the only field Care may write back
 * is [acknowledgedBy]. That asymmetry is enforced in the security rules, which is
 * what keeps the alert history trustworthy.
 */
data class CareEvent(
    val id: String = "",
    /** Stored as the enum *name* so an unknown future type degrades to [CareEventType.UNKNOWN]. */
    val type: String = CareEventType.UNKNOWN.name,
    val createdAt: Long = 0L,
    /** Human-readable detail, e.g. "Metformin, 12:00". */
    val detail: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Uids of caregivers who have seen it. Empty means nobody has acknowledged yet. */
    val acknowledgedBy: List<String> = emptyList(),
) {
    val eventType: CareEventType get() = CareEventType.fromName(type)
}

/**
 * Ordered by urgency, most severe first — [Severity] drives both the colour of the
 * stripe in the alert feed and whether the push notification bypasses Do Not Disturb.
 */
enum class CareEventType(
    val severity: Severity,
) {
    SOS(Severity.CRITICAL),
    FALL_DETECTED(Severity.CRITICAL),
    MISSED_DOSE(Severity.WARNING),
    LOW_BATTERY(Severity.WARNING),
    DOSE_TAKEN(Severity.INFO),
    PAIRED(Severity.INFO),

    /** Forward compatibility: an event type this build does not recognise. */
    UNKNOWN(Severity.INFO),
    ;

    companion object {
        fun fromName(name: String): CareEventType = entries.firstOrNull { it.name == name } ?: UNKNOWN
    }
}

enum class Severity {
    CRITICAL,
    WARNING,
    INFO,
}
