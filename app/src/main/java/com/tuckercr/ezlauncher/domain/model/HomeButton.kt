package com.tuckercr.ezlauncher.domain.model

/**
 * All possible quick-action buttons on the Home screen.
 *
 * The user can enable/disable each one from the Customise screen.
 * Ordering here determines the default display order (left→right, top→bottom).
 *
 * [defaultEnabled] = false means the button is opt-in — it won't appear on
 * a fresh install or after an upgrade until the user (or caregiver) enables it.
 */
enum class HomeButton(
    val defaultLabel: String,
    val defaultEnabled: Boolean = true,
) {
    PHONE("Phone"),
    TEXT("Text"),
    CAMERA("Camera"),
    MAGNIFIER("Magnifier"),
    ALL_APPS("All Apps"),
    FLASHLIGHT("Flashlight"),
    WEB("Web", defaultEnabled = false),
    MAPS("Maps", defaultEnabled = false),
    EMAIL("Email", defaultEnabled = false),
    PHOTOS("Photos", defaultEnabled = false),
    YOUTUBE("YouTube", defaultEnabled = false),
    CALCULATOR("Calculator", defaultEnabled = false),
}
