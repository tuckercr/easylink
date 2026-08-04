package com.fangjet.shared.config

/**
 * Firebase Remote Config parameter keys.
 *
 * These strings must match the parameter names in the Remote Config console
 * exactly — a typo just means the console value is silently ignored and the
 * hardcoded default wins. Keep them here so both the console template
 * (`firebase/remoteconfig.template.json`) and the app read from one source.
 *
 * See [SettingsDefaults] for the value each key controls and its fallback.
 */
object RemoteConfigKeys {
    const val SOS_BUTTON_VISIBLE_BY_DEFAULT = "sos_button_visible_by_default"
    const val VOICE_BUTTON_VISIBLE_BY_DEFAULT = "voice_button_visible_by_default"
    const val HIGH_CONTRAST_BY_DEFAULT = "high_contrast_by_default"
    const val FALL_DETECTION_ENABLED_BY_DEFAULT = "fall_detection_enabled_by_default"
    const val FALL_SENSITIVITY_DEFAULT = "fall_sensitivity_default"
    const val SOS_HOLD_DURATION_MS = "sos_hold_duration_ms"
    const val SOS_COUNTDOWN_SECONDS = "sos_countdown_seconds"
    const val FAVORITE_APPS_MAX_COUNT = "favorite_apps_max_count"

    // Feature kill switches — false removes the feature entirely (UI + settings),
    // overriding any stored preference. See SettingsDefaults for semantics.
    const val VOICE_COMMANDS_FEATURE_ENABLED = "voice_commands_feature_enabled"
    const val FAVORITE_APPS_FEATURE_ENABLED = "apps_row_feature_enabled"

    /** Comma-separated package names: the Apps Row's cold-start fill pool. */
    const val FAVORITE_APPS_CURATED_POOL = "apps_row_curated_pool"

    /** Every key, so callers can seed in-app defaults and templates without repeating the list. */
    val ALL: List<String> = listOf(
        SOS_BUTTON_VISIBLE_BY_DEFAULT,
        VOICE_BUTTON_VISIBLE_BY_DEFAULT,
        HIGH_CONTRAST_BY_DEFAULT,
        FALL_DETECTION_ENABLED_BY_DEFAULT,
        FALL_SENSITIVITY_DEFAULT,
        SOS_HOLD_DURATION_MS,
        SOS_COUNTDOWN_SECONDS,
        FAVORITE_APPS_MAX_COUNT,
        VOICE_COMMANDS_FEATURE_ENABLED,
        FAVORITE_APPS_FEATURE_ENABLED,
        FAVORITE_APPS_CURATED_POOL,
    )
}
