package com.tuckercr.ezlauncher.domain.model

import android.net.Uri

/**
 * A single item in the unified Inbox view — either a call log entry or an
 * SMS conversation thread.
 *
 * ## Why a sealed class instead of a common interface?
 * Call and Message items share identity fields ([displayName], [phoneNumber],
 * [timestamp]) but differ significantly in what the UI needs to render.
 * A sealed class lets the adapter do exhaustive `when` branches with full
 * type safety, and lets ViewHolders bind domain types directly without
 * casting.
 *
 * ## Sort order
 * Both variants implement [Comparable] via [timestamp] so a combined list
 * can be sorted with a single `sortedByDescending { it.timestamp }`.
 */
sealed class InboxItem {
    abstract val id: Long
    abstract val displayName: String
    abstract val phoneNumber: String
    abstract val photoUri: Uri?
    abstract val timestamp: Long     // epoch milliseconds

    /** Initials placeholder (max 2 chars) when [photoUri] is null. */
    val initials: String
        get() = displayName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "#" }

    // ── Call ──────────────────────────────────────────────────────────────

    /**
     * One call-log entry.
     *
     * @param type            Direction and outcome (INCOMING, MISSED, etc.)
     * @param durationSeconds Call duration; 0 for missed/rejected calls.
     */
    data class Call(
        override val id: Long,
        override val displayName: String,
        override val phoneNumber: String,
        override val photoUri: Uri?,
        override val timestamp: Long,
        val type: CallType,
        val durationSeconds: Long,
    ) : InboxItem() {
        val isMissed: Boolean get() = type == CallType.MISSED
    }

    // ── Message ───────────────────────────────────────────────────────────

    /**
     * The most-recent SMS message in a conversation thread.
     *
     * One [Message] row represents an entire thread, not a single message —
     * tapping it opens the full thread in the system messaging app.
     *
     * @param snippet      Preview of the most recent message body.
     * @param isRead       False if the thread has unread messages.
     * @param threadId     SMS thread ID; used to open the conversation.
     * @param isIncoming   True if the most recent message was received.
     */
    data class Message(
        override val id: Long,
        override val displayName: String,
        override val phoneNumber: String,
        override val photoUri: Uri?,
        override val timestamp: Long,
        val snippet: String,
        val isRead: Boolean,
        val threadId: Long,
        val isIncoming: Boolean,
    ) : InboxItem()
}
