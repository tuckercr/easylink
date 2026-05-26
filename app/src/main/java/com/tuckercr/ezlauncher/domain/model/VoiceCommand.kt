package com.tuckercr.ezlauncher.domain.model

/**
 * Represents a recognised voice command after parsing raw speech text.
 *
 * Parsing is intentionally decoupled from execution: [VoiceCommandParser]
 * converts a raw transcript into one of these types, and
 * [VoiceCommandViewModel] decides how to fulfil each one.
 */
sealed class VoiceCommand {

    // ── Person-directed ───────────────────────────────────────────────────────

    /** "Call [name]" — look up the contact and dial. */
    data class Call(
        val name: String,
    ) : VoiceCommand()

    /** "Text [name]" / "Message [name]" — open SMS composer for the contact. */
    data class Text(
        val name: String,
    ) : VoiceCommand()

    // ── App / feature launch ──────────────────────────────────────────────────

    /** "Open camera" / "Take a photo" — launch the system camera app. */
    data object OpenCamera : VoiceCommand()

    /** "Open [app name]" / "Launch [app name]" — find and launch an installed app. */
    data class OpenApp(
        val appName: String,
    ) : VoiceCommand()

    /** "All apps" / "Show apps" — navigate to the full app list screen. */
    data object OpenAllApps : VoiceCommand()

    // ── Device controls ───────────────────────────────────────────────────────

    /** "Flashlight on" / "Torch on" — ensure the torch is on. */
    data object FlashlightOn : VoiceCommand()

    /** "Flashlight off" / "Torch off" — ensure the torch is off. */
    data object FlashlightOff : VoiceCommand()

    /** "Flashlight" / "Torch" — toggle the current torch state. */
    data object ToggleFlashlight : VoiceCommand()

    // ── Navigation ────────────────────────────────────────────────────────────

    /** "Home" / "Go home" — return to the home screen. */
    data object GoHome : VoiceCommand()

    // ── Fallback ──────────────────────────────────────────────────────────────

    /** Speech was recognised but matched no known pattern. */
    data class Unrecognized(
        val text: String,
    ) : VoiceCommand()
}
