package com.fangjet.launcher.domain.model

import androidx.annotation.StringRes
import com.fangjet.launcher.R

/**
 * All possible quick-action buttons on the Home screen.
 *
 * The user can enable/disable each one from the Customise screen.
 * Ordering here determines the default display order (left→right, top→bottom).
 *
 * [labelRes] points at the same string resource the Home screen renders, so
 * the toggle in Settings and the button itself can never drift apart (and both
 * translate together).
 *
 * [defaultEnabled] = false means the button is opt-in — it won't appear on
 * a fresh install or after an upgrade until the user (or caregiver) enables it.
 */
enum class HomeButton(
    @param:StringRes val labelRes: Int,
    val defaultEnabled: Boolean = true,
) {
    PHONE(R.string.phone),
    TEXT(R.string.sms),
    CAMERA(R.string.camera),
    MAGNIFIER(R.string.magnifier),
    ALL_APPS(R.string.apps),
    SPEED_DIAL(R.string.people),
    MEDICATIONS(R.string.nav_meds),
    FLASHLIGHT(R.string.flash_light),
    WEB(R.string.web),
    MAPS(R.string.maps, defaultEnabled = false),
    EMAIL(R.string.email, defaultEnabled = false),
    PHOTOS(R.string.photos, defaultEnabled = false),
    YOUTUBE(R.string.youtube, defaultEnabled = false),
    CALCULATOR(R.string.calculator, defaultEnabled = false),
}
