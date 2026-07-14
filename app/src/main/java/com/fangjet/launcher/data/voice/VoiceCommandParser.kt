package com.fangjet.launcher.data.voice

import com.fangjet.launcher.domain.model.VoiceCommand
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts raw speech transcript text into a typed [VoiceCommand].
 *
 * Design notes:
 *  - All matching is case-insensitive and trims whitespace.
 *  - Specific patterns (e.g. "open camera") must appear **before** their
 *    more general prefix (e.g. "open [app]") in the when block.
 *  - The class is pure (no I/O, no Android deps) and easily unit-testable.
 */
@Singleton
class VoiceCommandParser @Inject constructor() {

    fun parse(raw: String): VoiceCommand {
        val t = raw.trim().lowercase()
        return when {
            // ── Call ──────────────────────────────────────────────────────────
            t.startsWith("call ") ->
                VoiceCommand.Call(t.removePrefix("call ").trim())

            // ── Text / SMS ────────────────────────────────────────────────────
            t.startsWith("text ") ->
                VoiceCommand.Text(t.removePrefix("text ").trim())

            t.startsWith("message ") ->
                VoiceCommand.Text(t.removePrefix("message ").trim())

            t.startsWith("sms ") ->
                VoiceCommand.Text(t.removePrefix("sms ").trim())

            t.startsWith("send a text to ") ->
                VoiceCommand.Text(t.removePrefix("send a text to ").trim())

            t.startsWith("send text to ") ->
                VoiceCommand.Text(t.removePrefix("send text to ").trim())

            // ── Camera (must precede "open [app]") ────────────────────────────
            t == "camera" ||
                t == "open camera" ||
                t == "take a photo" ||
                t == "take photo" ||
                t == "take a picture" ||
                t == "take picture" ->
                VoiceCommand.OpenCamera

            // ── Flashlight ────────────────────────────────────────────────────
            t == "flashlight on" ||
                t == "torch on" ||
                t == "turn on flashlight" ||
                t == "turn on the flashlight" ||
                t == "turn on torch" ||
                t == "turn on the torch" ||
                t == "turn the flashlight on" ||
                t == "turn the torch on" ->
                VoiceCommand.FlashlightOn

            t == "flashlight off" ||
                t == "torch off" ||
                t == "turn off flashlight" ||
                t == "turn off the flashlight" ||
                t == "turn off torch" ||
                t == "turn off the torch" ||
                t == "turn the flashlight off" ||
                t == "turn the torch off" ->
                VoiceCommand.FlashlightOff

            t == "flashlight" || t == "torch" || t == "toggle flashlight" ->
                VoiceCommand.ToggleFlashlight

            // ── All apps (must precede "open [app]") ─────────────────────────
            t == "apps" ||
                t == "all apps" ||
                t == "show apps" ||
                t == "open apps" ||
                t == "show all apps" ||
                t == "open all apps" ->
                VoiceCommand.OpenAllApps

            // ── Home ──────────────────────────────────────────────────────────
            t == "home" || t == "go home" || t == "go to home" ->
                VoiceCommand.GoHome

            // ── Open / launch app (generic — must be after specific cases) ────
            t.startsWith("open ") ->
                VoiceCommand.OpenApp(t.removePrefix("open ").trim())

            t.startsWith("launch ") ->
                VoiceCommand.OpenApp(t.removePrefix("launch ").trim())

            t.startsWith("start ") ->
                VoiceCommand.OpenApp(t.removePrefix("start ").trim())

            else -> VoiceCommand.Unrecognized(raw)
        }
    }
}
