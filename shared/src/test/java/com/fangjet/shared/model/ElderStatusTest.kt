package com.fangjet.shared.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElderStatusTest {

    private val now = 1_000_000_000L

    @Test
    fun `is online at the edge of the window but not past it`() {
        val atEdge = ElderStatus(lastSeenAt = now - ElderStatus.ONLINE_WINDOW_MS)
        val pastEdge = ElderStatus(lastSeenAt = now - ElderStatus.ONLINE_WINDOW_MS - 1)

        assertTrue(atEdge.isOnline(now))
        assertFalse(pastEdge.isOnline(now))
    }

    @Test
    fun `a never-seen device is offline`() {
        assertFalse(ElderStatus().isOnline(now))
    }

    @Test
    fun `a clock skewed into the future still reads as online`() {
        // Device clocks drift; a heartbeat stamped slightly ahead of us should
        // not flip the dashboard to "offline".
        assertTrue(ElderStatus(lastSeenAt = now + 60_000).isOnline(now))
    }
}
