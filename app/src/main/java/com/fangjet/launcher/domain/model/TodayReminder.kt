package com.fangjet.launcher.domain.model

import java.time.LocalDateTime

/**
 * A single reminder slot for today's schedule view.
 *
 * Flattens the Medication → reminderTimes relationship into one object
 * per reminder, ready for the UI to display chronologically.
 */
data class TodayReminder(
    val medication: Medication,
    val scheduledAt: LocalDateTime,
    val status: Status,
) : Comparable<TodayReminder> {

    enum class Status { UPCOMING, TAKEN, SNOOZED, MISSED, OVERDUE }

    override fun compareTo(other: TodayReminder): Int = scheduledAt.compareTo(other.scheduledAt)

    val isPast: Boolean get() = scheduledAt.isBefore(LocalDateTime.now())

    companion object {
        /** Statuses that mean the user can no longer take action on this slot. */
        val TERMINAL_STATUSES = setOf(Status.TAKEN, Status.MISSED)
    }
}
