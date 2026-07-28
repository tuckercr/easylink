package com.fangjet.launcher.data.pairing

import com.fangjet.shared.model.ContactConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactConfigMapperTest {

    // ── parse ────────────────────────────────────────────────────────────────

    @Test
    fun `parses a well-formed firestore array`() {
        val raw = listOf(
            mapOf(
                "id" to "a",
                "name" to "Sarah",
                "phoneNumber" to "5551234",
                "relationship" to "Daughter",
                "position" to 0L,
            ),
            mapOf(
                "id" to "b",
                "name" to "Robert",
                "phoneNumber" to "5555678",
                "relationship" to "",
                "position" to 1L,
            ),
        )
        val parsed = ContactConfigMapper.parse(raw)
        assertEquals(2, parsed.size)
        assertEquals("Sarah", parsed[0].name)
        assertEquals(1, parsed[1].position)
    }

    @Test
    fun `sorts by position regardless of array order`() {
        val raw = listOf(
            mapOf("id" to "b", "name" to "Second", "position" to 1L),
            mapOf("id" to "a", "name" to "First", "position" to 0L),
        )
        assertEquals(listOf("First", "Second"), ContactConfigMapper.parse(raw).map { it.name })
    }

    @Test
    fun `drops junk entries instead of crashing`() {
        val raw = listOf(
            "not a map",
            mapOf("id" to "x"), // no name
            mapOf("name" to "  "), // blank name
            mapOf("name" to "Valid", "phoneNumber" to "5550000"),
            null,
        )
        val parsed = ContactConfigMapper.parse(raw)
        assertEquals(1, parsed.size)
        assertEquals("Valid", parsed[0].name)
    }

    @Test
    fun `non-list input yields empty`() {
        assertTrue(ContactConfigMapper.parse(null).isEmpty())
        assertTrue(ContactConfigMapper.parse("garbage").isEmpty())
        assertTrue(ContactConfigMapper.parse(42).isEmpty())
    }

    // ── toEntities ───────────────────────────────────────────────────────────

    @Test
    fun `first by position becomes the primary contact`() {
        val entities = ContactConfigMapper.toEntities(
            listOf(
                ContactConfig(id = "b", name = "Backup", phoneNumber = "2", position = 3),
                ContactConfig(id = "a", name = "Main", phoneNumber = "1", position = 0),
            ),
        )
        assertEquals("Main", entities[0].name)
        assertTrue(entities[0].isPrimary)
        assertFalse(entities[1].isPrimary)
    }

    @Test
    fun `entities use autogenerate ids`() {
        val entities = ContactConfigMapper.toEntities(
            listOf(ContactConfig(id = "cloud-id", name = "A", phoneNumber = "5550001")),
        )
        assertEquals(0L, entities[0].id)
    }

    @Test
    fun `empty list maps to empty list`() {
        assertTrue(ContactConfigMapper.toEntities(emptyList()).isEmpty())
    }
}
