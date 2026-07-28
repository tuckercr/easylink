package com.fangjet.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PairingCodeTest {

    @Test
    fun `generate always produces exactly six digits`() {
        repeat(500) {
            val code = PairingCode.generate()
            assertEquals(6, code.length)
            assertTrue("'$code' should be all digits", code.all { it.isDigit() })
        }
    }

    @Test
    fun `generate preserves leading zeros`() {
        // A code of "000123" must stay six characters — treating codes as Int
        // would silently turn this into "123" and break redemption.
        val allZeroSeed = Random(0)
        repeat(500) {
            assertEquals(6, PairingCode.generate(allZeroSeed).length)
        }
    }

    @Test
    fun `isValidFormat rejects wrong length and non-digits`() {
        assertTrue(PairingCode.isValidFormat("012345"))
        assertFalse(PairingCode.isValidFormat("12345"))
        assertFalse(PairingCode.isValidFormat("1234567"))
        assertFalse(PairingCode.isValidFormat("12345a"))
        assertFalse(PairingCode.isValidFormat(""))
        assertFalse(PairingCode.isValidFormat("12 345"))
    }

    @Test
    fun `isExpired is inclusive at the expiry instant`() {
        val expiry = 1_000L
        assertFalse(PairingCode.isExpired(expiry, nowMillis = 999L))
        assertTrue(PairingCode.isExpired(expiry, nowMillis = 1_000L))
        assertTrue(PairingCode.isExpired(expiry, nowMillis = 1_001L))
    }

    @Test
    fun `formatForDisplay splits into two groups and leaves odd input alone`() {
        assertEquals("123 456", PairingCode.formatForDisplay("123456"))
        assertEquals("000 001", PairingCode.formatForDisplay("000001"))
        assertEquals("12345", PairingCode.formatForDisplay("12345"))
    }
}
