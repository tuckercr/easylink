package com.fangjet.shared.config

/**
 * The factory-default values for settings whose defaults we want to tune from the
 * server without shipping an app update.
 *
 * ## Where this sits in the precedence chain
 *
 * ```
 * SettingsDefaults.HARDCODED         ← compiled-in fallback, always works offline
 *   └─ overridden by Remote Config    ← global defaults set in the Firebase console
 *        └─ overridden by ElderConfig ← a caregiver's per-elder choices (Firestore)
 *             └─ overridden by the user's own local changes (DataStore)
 * ```
 *
 * A default only decides what happens *before* the user or caregiver has chosen.
 * Changing a value here (or in the console) never overrides a choice someone has
 * already made — it only moves the starting point for people who haven't.
 *
 * ## Adding a new tunable default
 *
 * 1. Add a field here with its fallback value in [HARDCODED].
 * 2. Add a key in [RemoteConfigKeys] and read it in [from].
 * 3. Add it to `firebase/remoteconfig.template.json`.
 * 4. Make the setting's data source fall back to this value instead of a literal.
 */
data class SettingsDefaults(
    /** Whether the full-width SOS button shows on a fresh install. */
    val sosButtonVisible: Boolean,
    /** Whether the "Say a Command" voice bar shows on a fresh install. */
    val voiceButtonVisible: Boolean,
    /** Whether high-contrast mode starts on. */
    val highContrast: Boolean,
    /** Whether fall detection starts on. */
    val fallDetectionEnabled: Boolean,
    /** Fall-detection sensitivity, as a `FallSensitivity` enum name (LOW/MEDIUM/HIGH). */
    val fallSensitivity: String,
    /** How long the SOS button must be held to fire, in milliseconds. */
    val sosHoldDurationMs: Long,
    /** Seconds the SOS countdown screen ticks before actually dispatching. */
    val sosCountdownSeconds: Int,
    /** How many apps the Home screen's Apps Row shows. */
    val favoriteAppsMaxCount: Int,
    /**
     * Feature kill switch: voice commands. Unlike [voiceButtonVisible] (a
     * *default* the user can override), false here removes the feature —
     * no Home button, no Settings toggle — regardless of stored preferences.
     */
    val voiceCommandsFeatureEnabled: Boolean,
    /** Feature kill switch: the Apps Row. Same semantics as the voice switch. */
    val favoriteAppsFeatureEnabled: Boolean,
    /**
     * Feature kill switch: the high-contrast theme. Defaults OFF — the feature
     * is parked pending a design pass, but stays resurrectable from the
     * console without shipping an update.
     */
    val highContrastFeatureEnabled: Boolean,
    /**
     * Cold-start fill order for the Apps Row before any launch history exists:
     * package names, most-likely-used first. May be longer than
     * [favoriteAppsMaxCount] — packages not installed on a device are skipped,
     * so a bigger pool just improves the odds of a sensible row.
     */
    val favoriteAppsCuratedPool: List<String>,
) {
    companion object {
        /**
         * The compiled-in fallback. These values are the app's behaviour when
         * Remote Config is unreachable or not yet configured, so they must match
         * the product's intended out-of-the-box defaults exactly.
         */
        val HARDCODED = SettingsDefaults(
            sosButtonVisible = true,
            voiceButtonVisible = true,
            highContrast = false,
            fallDetectionEnabled = false,
            fallSensitivity = "MEDIUM",
            sosHoldDurationMs = 3_000L,
            sosCountdownSeconds = 5,
            favoriteAppsMaxCount = 12,
            voiceCommandsFeatureEnabled = true,
            favoriteAppsFeatureEnabled = true,
            highContrastFeatureEnabled = false,
            favoriteAppsCuratedPool = listOf(
                "com.whatsapp",
                "com.google.android.apps.messaging",
                "com.google.android.apps.photos",
                "com.google.android.youtube",
                "com.facebook.katana",
                "com.android.chrome",
                "com.google.android.gm",
                "com.google.android.apps.maps",
                "com.google.android.calendar",
                "com.spotify.music",
                "com.audible.application",
                "com.netflix.mediaclient",
                "com.amazon.kindle",
                "com.instagram.android",
                "com.google.android.contacts",
                "com.google.android.deskclock",
            ),
        )

        /** Lower bound for [sosHoldDurationMs]; a too-short hold defeats the accidental-press guard. */
        private const val MIN_SOS_HOLD_MS = 1_000L

        /** Upper bound; a too-long hold is a usability failure for someone in distress. */
        private const val MAX_SOS_HOLD_MS = 10_000L

        private val VALID_SENSITIVITIES = setOf("LOW", "MEDIUM", "HIGH")

        /** Countdown must leave a real cancel window but not delay help absurdly. */
        private val SOS_COUNTDOWN_RANGE = 3..30

        /** Row length: enough to be useful, not a second app drawer. */
        private val FAVORITE_APPS_RANGE = 4..24

        /** Storage sanity cap for the curated pool — a pool needs no more. */
        private const val MAX_CURATED_POOL = 50

        /** Loose Android package-name shape: dot-separated java-ish identifiers. */
        private val PACKAGE_NAME_REGEX =
            Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

        /**
         * Parses a comma-separated package list from the console. Entries are
         * trimmed, validated against [PACKAGE_NAME_REGEX], and de-duplicated in
         * order. Returns null when nothing valid remains, so a broken console
         * value falls back to the hardcoded pool instead of emptying the row.
         */
        fun parseCuratedPool(csv: String?): List<String>? =
            csv
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.matches(PACKAGE_NAME_REGEX) }
                ?.distinct()
                ?.take(MAX_CURATED_POOL)
                ?.takeIf { it.isNotEmpty() }

        /**
         * Builds a [SettingsDefaults] from a Remote Config accessor, falling back
         * to [HARDCODED] for any value that is missing or out of range.
         *
         * The reader returns null for a key Remote Config doesn't have; each value
         * is validated so a fat-fingered console entry (a negative hold time, a
         * misspelled sensitivity) can never put the app into a nonsensical state.
         */
        fun from(reader: RemoteValueReader): SettingsDefaults {
            val sensitivity = reader
                .string(RemoteConfigKeys.FALL_SENSITIVITY_DEFAULT)
                ?.uppercase()
                ?.takeIf { it in VALID_SENSITIVITIES }
                ?: HARDCODED.fallSensitivity

            val holdMs = reader
                .long(RemoteConfigKeys.SOS_HOLD_DURATION_MS)
                ?.takeIf { it in MIN_SOS_HOLD_MS..MAX_SOS_HOLD_MS }
                ?: HARDCODED.sosHoldDurationMs

            val countdown = reader
                .long(RemoteConfigKeys.SOS_COUNTDOWN_SECONDS)
                ?.toInt()
                ?.takeIf { it in SOS_COUNTDOWN_RANGE }
                ?: HARDCODED.sosCountdownSeconds

            val favoriteMax = reader
                .long(RemoteConfigKeys.FAVORITE_APPS_MAX_COUNT)
                ?.toInt()
                ?.takeIf { it in FAVORITE_APPS_RANGE }
                ?: HARDCODED.favoriteAppsMaxCount

            return SettingsDefaults(
                sosButtonVisible = reader.boolean(RemoteConfigKeys.SOS_BUTTON_VISIBLE_BY_DEFAULT)
                    ?: HARDCODED.sosButtonVisible,
                voiceButtonVisible = reader.boolean(RemoteConfigKeys.VOICE_BUTTON_VISIBLE_BY_DEFAULT)
                    ?: HARDCODED.voiceButtonVisible,
                highContrast = reader.boolean(RemoteConfigKeys.HIGH_CONTRAST_BY_DEFAULT)
                    ?: HARDCODED.highContrast,
                fallDetectionEnabled = reader.boolean(RemoteConfigKeys.FALL_DETECTION_ENABLED_BY_DEFAULT)
                    ?: HARDCODED.fallDetectionEnabled,
                fallSensitivity = sensitivity,
                sosHoldDurationMs = holdMs,
                sosCountdownSeconds = countdown,
                favoriteAppsMaxCount = favoriteMax,
                voiceCommandsFeatureEnabled = reader.boolean(RemoteConfigKeys.VOICE_COMMANDS_FEATURE_ENABLED)
                    ?: HARDCODED.voiceCommandsFeatureEnabled,
                favoriteAppsFeatureEnabled = reader.boolean(RemoteConfigKeys.FAVORITE_APPS_FEATURE_ENABLED)
                    ?: HARDCODED.favoriteAppsFeatureEnabled,
                highContrastFeatureEnabled = reader.boolean(RemoteConfigKeys.HIGH_CONTRAST_FEATURE_ENABLED)
                    ?: HARDCODED.highContrastFeatureEnabled,
                favoriteAppsCuratedPool = parseCuratedPool(
                    reader.string(RemoteConfigKeys.FAVORITE_APPS_CURATED_POOL),
                ) ?: HARDCODED.favoriteAppsCuratedPool,
            )
        }
    }
}

/**
 * A minimal read interface over a key-value config source (Firebase Remote Config
 * in production, a plain map in tests).
 *
 * Each method returns null when the key is absent or the stored value can't be
 * read as the requested type, so [SettingsDefaults.from] can fall back cleanly.
 * Keeping this abstraction in `:shared` means all the mapping and validation
 * logic is unit-testable on the JVM with no Firebase dependency.
 */
interface RemoteValueReader {
    fun boolean(key: String): Boolean?

    fun long(key: String): Long?

    fun string(key: String): String?
}
