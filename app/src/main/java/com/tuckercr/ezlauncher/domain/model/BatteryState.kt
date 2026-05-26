package com.tuckercr.ezlauncher.domain.model

/**
 * Domain model for battery state.
 *
 * Encapsulates all battery information the UI needs so that
 * [HomeViewModel] never has to touch Android broadcast plumbing directly.
 */
data class BatteryState(
    /** Battery level in the range 0–100. */
    val levelPercent: Int,
    /** True when the device is connected to a power source. */
    val isCharging: Boolean,
) {
    companion object {
        /** Sentinel value used before the first broadcast arrives. */
        val Unknown = BatteryState(levelPercent = -1, isCharging = false)
    }

    val isLow: Boolean get() = levelPercent in 0..20
}
