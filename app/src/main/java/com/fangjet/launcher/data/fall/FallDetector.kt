package com.fangjet.launcher.data.fall

import android.os.SystemClock
import com.fangjet.launcher.domain.model.FallSensitivity
import kotlin.math.sqrt

/**
 * Stateless-looking fall detector implemented as a small state machine.
 *
 * ## Algorithm
 *
 * 1. **Free-fall phase** — accelerometer magnitude drops below [FallSensitivity.freeFallThreshold]
 *    for at least [FallSensitivity.minFreeFallMs] milliseconds.
 *    (During true free-fall the sensor reads near-zero because the device
 *    and sensor are both falling at the same rate.)
 *
 * 2. **Impact phase** — within [IMPACT_WINDOW_MS] after free-fall the
 *    magnitude spikes above [FallSensitivity.impactThreshold], indicating
 *    the body/device hit a surface.
 *
 * Call [process] on every raw `SensorEvent` from TYPE_ACCELEROMETER (or
 * TYPE_LINEAR_ACCELERATION). Returns **true** the first time a fall is
 * confirmed; the machine then resets automatically.
 *
 * Not thread-safe — call from a single sensor-callback thread.
 *
 * @param clock Supplies the current monotonic timestamp in milliseconds.
 *   Defaults to [SystemClock.elapsedRealtime]. Override in tests to control
 *   time without real waiting.
 */
class FallDetector(
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {

    companion object {
        /** How long (ms) to wait for an impact after the free-fall phase. */
        private const val IMPACT_WINDOW_MS = 1_500L
    }

    private enum class State { NORMAL, FREE_FALL, IMPACT_WINDOW }

    private var state = State.NORMAL
    private var freeFallStartMs = 0L
    private var impactWindowStart = 0L

    var sensitivity: FallSensitivity = FallSensitivity.MEDIUM

    /**
     * Feed a single accelerometer reading.
     *
     * @return true if a fall was just confirmed, false otherwise.
     */
    fun process(
        x: Float,
        y: Float,
        z: Float,
    ): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = clock()

        return when (state) {
            State.NORMAL -> {
                if (magnitude < sensitivity.freeFallThreshold) {
                    state = State.FREE_FALL
                    freeFallStartMs = now
                }
                false
            }

            State.FREE_FALL -> {
                if (magnitude >= sensitivity.freeFallThreshold) {
                    val duration = now - freeFallStartMs
                    if (duration >= sensitivity.minFreeFallMs) {
                        // Valid free-fall ended → enter impact window
                        state = State.IMPACT_WINDOW
                        impactWindowStart = now
                        // The same sample that ended free-fall might be the impact
                        if (magnitude > sensitivity.impactThreshold) {
                            state = State.NORMAL
                            return true
                        }
                    } else {
                        // Too short — not a real free-fall
                        state = State.NORMAL
                    }
                }
                false
            }

            State.IMPACT_WINDOW -> {
                when {
                    magnitude > sensitivity.impactThreshold -> {
                        state = State.NORMAL
                        true
                    }

                    now - impactWindowStart > IMPACT_WINDOW_MS -> {
                        // Window expired with no impact → not a fall
                        state = State.NORMAL
                        false
                    }

                    else -> false
                }
            }
        }
    }

    /** Reset to initial state (e.g. after user dismisses an alert). */
    fun reset() {
        state = State.NORMAL
    }
}
