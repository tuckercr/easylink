package com.tuckercr.ezlauncher.domain.model

/**
 * All possible quick-action buttons on the Home screen.
 *
 * The user can enable/disable each one from the Customise screen.
 * Ordering here determines the default display order (left→right, top→bottom).
 */
enum class HomeButton(val defaultLabel: String) {
    PHONE("Phone"),
    TEXT("Text"),
    CAMERA("Camera"),
    MAGNIFIER("Magnifier"),
    ALL_APPS("All Apps"),
    FLASHLIGHT("Flashlight"),
}
