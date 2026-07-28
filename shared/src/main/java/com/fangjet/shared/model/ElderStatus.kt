package com.fangjet.shared.model

/**
 * The launcher's heartbeat. Written by the launcher, read by Care.
 *
 * Deliberately small and overwritten in place rather than appended to — this
 * document is written on a schedule, and history belongs in [CareEvent], not here.
 */
data class ElderStatus(
    /** Epoch millis of the last write. Care shows "Online" within [ONLINE_WINDOW_MS]. */
    val lastSeenAt: Long = 0L,
    /** 0–100, or -1 when unknown. */
    val batteryPercent: Int = -1,
    val isCharging: Boolean = false,
    /** Doses marked taken today, and how many were scheduled. Drives the "2 of 3" ring. */
    val dosesTakenToday: Int = 0,
    val dosesScheduledToday: Int = 0,
    /** Launcher versionName, so Care can warn about an out-of-date install. */
    val appVersion: String = "",
) {
    companion object {
        /** A heartbeat newer than this counts as "Online" in the Care dashboard. */
        const val ONLINE_WINDOW_MS = 15 * 60 * 1_000L
    }

    fun isOnline(nowMillis: Long): Boolean = nowMillis - lastSeenAt <= ONLINE_WINDOW_MS
}
