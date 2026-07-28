package com.fangjet.launcher.data.notifications

import com.fangjet.launcher.data.notifications.NotificationBadgeRepository.Companion.NotificationInfo
import com.fangjet.launcher.data.notifications.NotificationBadgeRepository.Companion.computeBadged
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationBadgeLogicTest {

    @Test
    fun `a normal notification badges its package`() {
        val badged = computeBadged(
            listOf(NotificationInfo("com.whatsapp", isOngoing = false, isGroupSummary = false)),
        )
        assertEquals(setOf("com.whatsapp"), badged)
    }

    @Test
    fun `ongoing notifications never badge — music playback is not news`() {
        val badged = computeBadged(
            listOf(NotificationInfo("com.spotify.music", isOngoing = true, isGroupSummary = false)),
        )
        assertTrue(badged.isEmpty())
    }

    @Test
    fun `group summaries never badge on their own`() {
        val badged = computeBadged(
            listOf(NotificationInfo("com.gmail", isOngoing = false, isGroupSummary = true)),
        )
        assertTrue(badged.isEmpty())
    }

    @Test
    fun `one real notification is enough even among excluded ones`() {
        val badged = computeBadged(
            listOf(
                NotificationInfo("com.gmail", isOngoing = false, isGroupSummary = true),
                NotificationInfo("com.gmail", isOngoing = false, isGroupSummary = false),
                NotificationInfo("com.spotify.music", isOngoing = true, isGroupSummary = false),
            ),
        )
        assertEquals(setOf("com.gmail"), badged)
    }

    @Test
    fun `empty input badges nothing`() {
        assertTrue(computeBadged(emptyList()).isEmpty())
    }
}
