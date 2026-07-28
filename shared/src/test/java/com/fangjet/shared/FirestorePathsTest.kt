package com.fangjet.shared

import org.junit.Assert.assertEquals
import org.junit.Test

class FirestorePathsTest {

    private val linkId = "link123"

    @Test
    fun `config and status resolve to documents, events to a collection`() {
        // Firestore requires an even segment count for documents and odd for
        // collections — getting this backwards fails only at runtime.
        assertEquals("links/link123/config/current", FirestorePaths.config(linkId))
        assertEquals("links/link123/status/current", FirestorePaths.status(linkId))
        assertEquals("links/link123/events", FirestorePaths.events(linkId))
        assertEquals("links/link123/events/evt1", FirestorePaths.event(linkId, "evt1"))
    }

    @Test
    fun `document paths have an even number of segments`() {
        listOf(
            FirestorePaths.link(linkId),
            FirestorePaths.config(linkId),
            FirestorePaths.status(linkId),
            FirestorePaths.event(linkId, "evt1"),
        ).forEach { path ->
            assertEquals("$path should address a document", 0, path.split("/").size % 2)
        }
    }

    @Test
    fun `collection paths have an odd number of segments`() {
        listOf(
            FirestorePaths.LINKS,
            FirestorePaths.events(linkId),
        ).forEach { path ->
            assertEquals("$path should address a collection", 1, path.split("/").size % 2)
        }
    }
}
