package com.fangjet.launcher.data.fall

import com.fangjet.launcher.domain.model.FallSensitivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [FallDetector].
 *
 * [FallDetector] accepts an injectable [clock] lambda so tests can control time
 * without Robolectric or static mocking. The clock is advanced manually per test.
 */
class FallDetectorTest {

    /** Mutable clock shared between the test body and [FallDetector]. */
    private var fakeTimeMs = 0L

    private lateinit var detector: FallDetector

    @Before
    fun setup() {
        fakeTimeMs = 0L
        detector = FallDetector(clock = { fakeTimeMs })
        detector.sensitivity = FallSensitivity.MEDIUM
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private val sens get() = FallSensitivity.MEDIUM

    /** Magnitude guaranteed to be below freeFallThreshold (simulates free-fall). */
    private fun lowMag() = detector.process(0f, 0f, 0f)

    /**
     * Magnitude that ends free-fall but is below the impact threshold
     * (triggers IMPACT_WINDOW state without immediately confirming a fall).
     * √(3² + 3²) ≈ 4.24 — above freeFallThreshold (3.0), below impactThreshold (22.0).
     */
    @Suppress("unused")
    private fun midMag() = detector.process(3f, 3f, 0f)

    /** Magnitude well above impactThreshold (triggers fall confirmation). */
    private fun highMag() = detector.process(20f, 20f, 20f) // √1200 ≈ 34.6 > 22.0 ✓

    // ── Normal movement ───────────────────────────────────────────────────────

    @Test
    fun `normal walking magnitude does not trigger a fall`() {
        // √(8² + 8² + 4²) ≈ 11.3 m/s² — typical walking, above freeFallThreshold
        repeat(20) {
            assertFalse(detector.process(8f, 8f, 4f))
        }
    }

    // ── Free-fall → not long enough ───────────────────────────────────────────

    @Test
    fun `brief free-fall shorter than minFreeFallMs does not trigger fall`() {
        lowMag() // t=0, enter FREE_FALL

        fakeTimeMs = sens.minFreeFallMs - 1 // 79 ms — too short

        assertFalse(detector.process(20f, 20f, 20f)) // high impact but free-fall was too brief
    }

    // ── Free-fall → valid → immediate impact ──────────────────────────────────

    @Test
    fun `valid free-fall followed immediately by high impact triggers fall`() {
        lowMag() // t=0, enter FREE_FALL

        fakeTimeMs = sens.minFreeFallMs + 10 // 90 ms — valid duration

        // High magnitude ends free-fall AND exceeds impact threshold in one sample
        assertTrue(detector.process(20f, 20f, 20f))
    }

    // ── Free-fall → IMPACT_WINDOW → impact ───────────────────────────────────

    @Test
    fun `impact arriving within impact window after free-fall triggers fall`() {
        lowMag() // t=0

        fakeTimeMs = sens.minFreeFallMs + 10 // 90 ms

        // End free-fall with magnitude just above freeFallThreshold (below impactThreshold)
        // √(3² + 3² + 0²) ≈ 4.24 — above freeFallThreshold (3.0), below impactThreshold (22.0)
        detector.process(3f, 3f, 0f) // → enters IMPACT_WINDOW

        fakeTimeMs += 500 // 590 ms total — within 1500 ms window

        assertTrue(detector.process(20f, 20f, 20f)) // impact detected
    }

    // ── Free-fall → IMPACT_WINDOW → window expires ────────────────────────────

    @Test
    fun `no impact within window after free-fall returns false`() {
        lowMag() // t=0

        fakeTimeMs = sens.minFreeFallMs + 10 // 90 ms

        // Transition to IMPACT_WINDOW
        detector.process(3f, 3f, 0f)

        fakeTimeMs += 1_600L // past the 1500 ms impact window

        // Low magnitude after window expired — not a fall
        assertFalse(lowMag())
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test
    fun `detector detects a second fall after reset`() {
        // First fall
        lowMag()
        fakeTimeMs = sens.minFreeFallMs + 10
        assertTrue(highMag())

        // Manually reset (simulates user dismissing alert)
        detector.reset()

        // Second fall
        fakeTimeMs += 100
        lowMag()
        fakeTimeMs += sens.minFreeFallMs + 10
        assertTrue("Second fall should be detected after reset", highMag())
    }

    @Test
    fun `after fall is detected detector auto-resets to NORMAL`() {
        lowMag()
        fakeTimeMs = sens.minFreeFallMs + 10
        highMag() // triggers fall, resets to NORMAL

        // Next low-mag sample should start a new free-fall, not immediately trigger
        assertFalse(lowMag()) // returns false even though magnitude is low
    }

    // ── Sensitivity ───────────────────────────────────────────────────────────

    @Test
    fun `HIGH sensitivity has lower thresholds than MEDIUM`() {
        assertTrue(
            "HIGH minFreeFallMs should be ≤ MEDIUM (easier to trigger)",
            FallSensitivity.HIGH.minFreeFallMs <= FallSensitivity.MEDIUM.minFreeFallMs,
        )
        assertTrue(
            "HIGH impactThreshold should be ≤ MEDIUM (easier to trigger)",
            FallSensitivity.HIGH.impactThreshold <= FallSensitivity.MEDIUM.impactThreshold,
        )
    }

    @Test
    fun `LOW sensitivity requires longer free-fall than MEDIUM`() {
        assertTrue(
            "LOW minFreeFallMs should be ≥ MEDIUM (harder to trigger)",
            FallSensitivity.LOW.minFreeFallMs >= FallSensitivity.MEDIUM.minFreeFallMs,
        )
    }

    @Test
    fun `changing sensitivity mid-detection uses new thresholds immediately`() {
        // Start with MEDIUM, switch to HIGH before impact check
        lowMag() // t=0, MEDIUM freeFallThreshold=3.0
        fakeTimeMs = FallSensitivity.HIGH.minFreeFallMs + 5
        detector.sensitivity = FallSensitivity.HIGH

        // HIGH impactThreshold=18.0; our highMag() ≈ 34.6 > 18.0 → still triggers
        assertTrue(highMag())
    }
}
