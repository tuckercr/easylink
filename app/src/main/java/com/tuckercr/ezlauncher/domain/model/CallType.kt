package com.tuckercr.ezlauncher.domain.model

import android.provider.CallLog

/**
 * Typed representation of a call log entry's direction and outcome.
 *
 * Maps from [CallLog.Calls.TYPE] integer constants so the rest of the
 * domain never imports Android framework classes.
 */
enum class CallType {
    /** Call received and answered. */
    INCOMING,

    /** Call placed by the user. */
    OUTGOING,

    /** Incoming call that was not answered. */
    MISSED,

    /** Call rejected by the user. */
    REJECTED,

    /** Number blocked — shown so the user can see it was silenced. */
    BLOCKED,

    /** Any other type reported by the system. */
    UNKNOWN,

    ;

    companion object {
        fun fromInt(type: Int): CallType =
            when (type) {
                CallLog.Calls.INCOMING_TYPE -> INCOMING
                CallLog.Calls.OUTGOING_TYPE -> OUTGOING
                CallLog.Calls.MISSED_TYPE -> MISSED
                CallLog.Calls.REJECTED_TYPE -> REJECTED
                CallLog.Calls.BLOCKED_TYPE -> BLOCKED
                else -> UNKNOWN
            }
    }
}
