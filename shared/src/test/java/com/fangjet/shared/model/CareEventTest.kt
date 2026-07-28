package com.fangjet.shared.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CareEventTest {

    @Test
    fun `unknown type names degrade to UNKNOWN rather than throwing`() {
        // A newer launcher may emit an event type this build of Care has never
        // heard of. That must not crash the alert feed.
        assertEquals(CareEventType.UNKNOWN, CareEventType.fromName("MEDICATION_REORDERED"))
        assertEquals(CareEventType.UNKNOWN, CareEventType.fromName(""))
    }

    @Test
    fun `round-trips through the stored name`() {
        CareEventType.entries.forEach { type ->
            assertEquals(type, CareEventType.fromName(type.name))
        }
    }

    @Test
    fun `event exposes its parsed type`() {
        val event = CareEvent(type = CareEventType.FALL_DETECTED.name)
        assertEquals(CareEventType.FALL_DETECTED, event.eventType)
        assertEquals(Severity.CRITICAL, event.eventType.severity)
    }

    @Test
    fun `SOS and falls are critical, dose reminders are not`() {
        assertEquals(Severity.CRITICAL, CareEventType.SOS.severity)
        assertEquals(Severity.CRITICAL, CareEventType.FALL_DETECTED.severity)
        assertEquals(Severity.WARNING, CareEventType.MISSED_DOSE.severity)
        assertEquals(Severity.INFO, CareEventType.DOSE_TAKEN.severity)
    }

    @Test
    fun `default event has no location`() {
        val event = CareEvent()
        assertEquals(null, event.latitude)
        assertEquals(null, event.longitude)
    }
}
