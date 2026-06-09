package com.tuckercr.ezlauncher.data.voice

import com.tuckercr.ezlauncher.domain.model.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [VoiceCommandParser].
 *
 * The parser is pure (no Android deps, no I/O) so these run as plain JVM tests.
 * Every branch of the `when` block is exercised; representative synonyms are
 * tested rather than every single phrase.
 */
class VoiceCommandParserTest {

    private lateinit var parser: VoiceCommandParser

    @Before
    fun setup() {
        parser = VoiceCommandParser()
    }

    // ── Call ─────────────────────────────────────────────────────────────────

    @Test
    fun `call command extracts contact name`() {
        val result = parser.parse("call Alice")
        assertEquals(VoiceCommand.Call("alice"), result)
    }

    @Test
    fun `call command is case-insensitive`() {
        val result = parser.parse("CALL Bob")
        assertEquals(VoiceCommand.Call("bob"), result)
    }

    @Test
    fun `call command trims whitespace`() {
        val result = parser.parse("  call  Carol  ")
        assertEquals(VoiceCommand.Call("carol"), result)
    }

    // ── Text / SMS ────────────────────────────────────────────────────────────

    @Test
    fun `text command extracts contact name`() {
        assertEquals(VoiceCommand.Text("alice"), parser.parse("text Alice"))
    }

    @Test
    fun `message command maps to Text`() {
        assertEquals(VoiceCommand.Text("bob"), parser.parse("message Bob"))
    }

    @Test
    fun `sms command maps to Text`() {
        assertEquals(VoiceCommand.Text("carol"), parser.parse("sms Carol"))
    }

    @Test
    fun `send a text to command maps to Text`() {
        assertEquals(VoiceCommand.Text("dave"), parser.parse("send a text to Dave"))
    }

    @Test
    fun `send text to command maps to Text`() {
        assertEquals(VoiceCommand.Text("eve"), parser.parse("send text to Eve"))
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    @Test
    fun `camera command recognised`() {
        assertEquals(VoiceCommand.OpenCamera, parser.parse("camera"))
    }

    @Test
    fun `open camera command recognised`() {
        assertEquals(VoiceCommand.OpenCamera, parser.parse("open camera"))
    }

    @Test
    fun `take a photo recognised`() {
        assertEquals(VoiceCommand.OpenCamera, parser.parse("take a photo"))
    }

    @Test
    fun `take picture recognised`() {
        assertEquals(VoiceCommand.OpenCamera, parser.parse("take picture"))
    }

    // ── Flashlight ────────────────────────────────────────────────────────────

    @Test
    fun `flashlight on command recognised`() {
        assertEquals(VoiceCommand.FlashlightOn, parser.parse("flashlight on"))
    }

    @Test
    fun `turn on flashlight recognised`() {
        assertEquals(VoiceCommand.FlashlightOn, parser.parse("turn on flashlight"))
    }

    @Test
    fun `turn on the torch recognised`() {
        assertEquals(VoiceCommand.FlashlightOn, parser.parse("turn on the torch"))
    }

    @Test
    fun `flashlight off command recognised`() {
        assertEquals(VoiceCommand.FlashlightOff, parser.parse("flashlight off"))
    }

    @Test
    fun `turn off torch recognised`() {
        assertEquals(VoiceCommand.FlashlightOff, parser.parse("turn off torch"))
    }

    @Test
    fun `flashlight toggle recognised`() {
        assertEquals(VoiceCommand.ToggleFlashlight, parser.parse("flashlight"))
    }

    @Test
    fun `torch toggle recognised`() {
        assertEquals(VoiceCommand.ToggleFlashlight, parser.parse("torch"))
    }

    // ── All Apps ──────────────────────────────────────────────────────────────

    @Test
    fun `apps command recognised`() {
        assertEquals(VoiceCommand.OpenAllApps, parser.parse("apps"))
    }

    @Test
    fun `all apps command recognised`() {
        assertEquals(VoiceCommand.OpenAllApps, parser.parse("all apps"))
    }

    @Test
    fun `show apps command recognised`() {
        assertEquals(VoiceCommand.OpenAllApps, parser.parse("show apps"))
    }

    @Test
    fun `open all apps not confused with generic open`() {
        // "open all apps" must map to OpenAllApps, not OpenApp("all apps")
        assertEquals(VoiceCommand.OpenAllApps, parser.parse("open all apps"))
    }

    // ── Home ─────────────────────────────────────────────────────────────────

    @Test
    fun `home command recognised`() {
        assertEquals(VoiceCommand.GoHome, parser.parse("home"))
    }

    @Test
    fun `go home command recognised`() {
        assertEquals(VoiceCommand.GoHome, parser.parse("go home"))
    }

    // ── Open / launch generic app ─────────────────────────────────────────────

    @Test
    fun `open command extracts app name`() {
        assertEquals(VoiceCommand.OpenApp("maps"), parser.parse("open maps"))
    }

    @Test
    fun `launch command extracts app name`() {
        assertEquals(VoiceCommand.OpenApp("spotify"), parser.parse("launch Spotify"))
    }

    @Test
    fun `start command extracts app name`() {
        assertEquals(VoiceCommand.OpenApp("settings"), parser.parse("start settings"))
    }

    // ── Unrecognised ──────────────────────────────────────────────────────────

    @Test
    fun `unknown input returns Unrecognized with original text`() {
        val result = parser.parse("abracadabra")
        assertTrue(result is VoiceCommand.Unrecognized)
        assertEquals("abracadabra", (result as VoiceCommand.Unrecognized).text)
    }

    @Test
    fun `empty string returns Unrecognized`() {
        val result = parser.parse("")
        assertTrue(result is VoiceCommand.Unrecognized)
    }
}
