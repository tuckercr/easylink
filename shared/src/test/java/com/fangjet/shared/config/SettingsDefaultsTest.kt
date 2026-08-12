package com.fangjet.shared.config

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDefaultsTest {

    /** A [RemoteValueReader] backed by a plain map — no Firebase needed. */
    private class MapReader(
        private val values: Map<String, Any>,
    ) : RemoteValueReader {
        override fun boolean(key: String) = values[key] as? Boolean

        override fun long(key: String) = values[key] as? Long

        override fun string(key: String) = values[key] as? String
    }

    @Test
    fun `empty config yields exactly the hardcoded defaults`() {
        val result = SettingsDefaults.from(MapReader(emptyMap()))
        assertEquals(SettingsDefaults.HARDCODED, result)
    }

    @Test
    fun `present values override the hardcoded defaults`() {
        val reader = MapReader(
            mapOf(
                RemoteConfigKeys.SOS_BUTTON_VISIBLE_BY_DEFAULT to false,
                RemoteConfigKeys.VOICE_BUTTON_VISIBLE_BY_DEFAULT to true,
                RemoteConfigKeys.HIGH_CONTRAST_BY_DEFAULT to true,
                RemoteConfigKeys.FALL_DETECTION_ENABLED_BY_DEFAULT to true,
                RemoteConfigKeys.FALL_SENSITIVITY_DEFAULT to "HIGH",
                RemoteConfigKeys.SOS_HOLD_DURATION_MS to 5_000L,
            ),
        )
        val result = SettingsDefaults.from(reader)

        assertEquals(false, result.sosButtonVisible)
        assertEquals(true, result.voiceButtonVisible)
        assertEquals(true, result.highContrast)
        assertEquals(true, result.fallDetectionEnabled)
        assertEquals("HIGH", result.fallSensitivity)
        assertEquals(5_000L, result.sosHoldDurationMs)
    }

    @Test
    fun `an unknown sensitivity falls back rather than propagating garbage`() {
        val reader = MapReader(mapOf(RemoteConfigKeys.FALL_SENSITIVITY_DEFAULT to "EXTREME"))
        assertEquals("MEDIUM", SettingsDefaults.from(reader).fallSensitivity)
    }

    @Test
    fun `sensitivity is normalised to upper case`() {
        val reader = MapReader(mapOf(RemoteConfigKeys.FALL_SENSITIVITY_DEFAULT to "high"))
        assertEquals("HIGH", SettingsDefaults.from(reader).fallSensitivity)
    }

    @Test
    fun `an out-of-range hold time is clamped back to the default`() {
        // A console typo must never make SOS impossible to trigger, or so twitchy
        // a stray touch fires it.
        assertEquals(
            3_000L,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.SOS_HOLD_DURATION_MS to -1L)))
                .sosHoldDurationMs,
        )
        assertEquals(
            3_000L,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.SOS_HOLD_DURATION_MS to 60_000L)))
                .sosHoldDurationMs,
        )
    }

    @Test
    fun `hold time at the range boundaries is accepted`() {
        assertEquals(
            1_000L,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.SOS_HOLD_DURATION_MS to 1_000L)))
                .sosHoldDurationMs,
        )
        assertEquals(
            10_000L,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.SOS_HOLD_DURATION_MS to 10_000L)))
                .sosHoldDurationMs,
        )
    }

    @Test
    fun `an out-of-range countdown falls back to the default`() {
        // Too short leaves no time to cancel; absurdly long delays real help.
        assertEquals(
            5,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.SOS_COUNTDOWN_SECONDS to 1L)))
                .sosCountdownSeconds,
        )
        assertEquals(
            5,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.SOS_COUNTDOWN_SECONDS to 300L)))
                .sosCountdownSeconds,
        )
        assertEquals(
            10,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.SOS_COUNTDOWN_SECONDS to 10L)))
                .sosCountdownSeconds,
        )
    }

    @Test
    fun `an out-of-range favorite apps count falls back to the default`() {
        assertEquals(
            12,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.FAVORITE_APPS_MAX_COUNT to 1L)))
                .favoriteAppsMaxCount,
        )
        assertEquals(
            12,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.FAVORITE_APPS_MAX_COUNT to 100L)))
                .favoriteAppsMaxCount,
        )
        assertEquals(
            8,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.FAVORITE_APPS_MAX_COUNT to 8L)))
                .favoriteAppsMaxCount,
        )
    }

    @Test
    fun `every key is covered by the ALL list`() {
        // Guards against adding a key but forgetting to register it for seeding.
        assertEquals(RemoteConfigKeys.ALL.size, RemoteConfigKeys.ALL.toSet().size)
        assertEquals(12, RemoteConfigKeys.ALL.size)
    }

    // ── Feature kill switches ────────────────────────────────────────────────

    @Test
    fun `feature switches default on and can be turned off from the console`() {
        assertEquals(true, SettingsDefaults.HARDCODED.voiceCommandsFeatureEnabled)
        assertEquals(true, SettingsDefaults.HARDCODED.favoriteAppsFeatureEnabled)
        // High contrast is parked: hidden by default, resurrectable from the console.
        assertEquals(false, SettingsDefaults.HARDCODED.highContrastFeatureEnabled)
        assertEquals(
            true,
            SettingsDefaults
                .from(MapReader(mapOf(RemoteConfigKeys.HIGH_CONTRAST_FEATURE_ENABLED to true)))
                .highContrastFeatureEnabled,
        )

        val reader = MapReader(
            mapOf(
                RemoteConfigKeys.VOICE_COMMANDS_FEATURE_ENABLED to false,
                RemoteConfigKeys.FAVORITE_APPS_FEATURE_ENABLED to false,
            ),
        )
        val result = SettingsDefaults.from(reader)
        assertEquals(false, result.voiceCommandsFeatureEnabled)
        assertEquals(false, result.favoriteAppsFeatureEnabled)
    }

    // ── Curated pool CSV parsing ─────────────────────────────────────────────

    @Test
    fun `curated pool parses a comma-separated list, trimming whitespace`() {
        val reader = MapReader(
            mapOf(
                RemoteConfigKeys.FAVORITE_APPS_CURATED_POOL to
                    "com.whatsapp, com.spotify.music ,com.android.chrome",
            ),
        )
        assertEquals(
            listOf("com.whatsapp", "com.spotify.music", "com.android.chrome"),
            SettingsDefaults.from(reader).favoriteAppsCuratedPool,
        )
    }

    @Test
    fun `curated pool drops invalid entries and de-duplicates`() {
        assertEquals(
            listOf("com.whatsapp", "com.spotify.music"),
            SettingsDefaults.parseCuratedPool(
                "com.whatsapp,not a package!,nodots,com.whatsapp,com.spotify.music,",
            ),
        )
    }

    @Test
    fun `a curated pool with nothing valid falls back to the hardcoded pool`() {
        val reader = MapReader(
            mapOf(RemoteConfigKeys.FAVORITE_APPS_CURATED_POOL to ", , garbage !!, ,"),
        )
        assertEquals(
            SettingsDefaults.HARDCODED.favoriteAppsCuratedPool,
            SettingsDefaults.from(reader).favoriteAppsCuratedPool,
        )
    }

    @Test
    fun `an absent curated pool falls back to the hardcoded pool`() {
        assertEquals(
            SettingsDefaults.HARDCODED.favoriteAppsCuratedPool,
            SettingsDefaults.from(MapReader(emptyMap())).favoriteAppsCuratedPool,
        )
    }
}
