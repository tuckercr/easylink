package com.fangjet.launcher.data.config

import com.fangjet.shared.config.SettingsDefaults

/**
 * Supplies the current [SettingsDefaults] for the app to fall back on.
 *
 * The default values a fresh install starts from — see [SettingsDefaults] for how
 * this fits into the precedence chain. Implementations must always return a usable
 * value synchronously ([current]) even before any network fetch has happened, so
 * settings reads never block or fail.
 */
interface SettingsDefaultsProvider {
    /**
     * The latest known defaults. Returns [SettingsDefaults.HARDCODED] until a
     * Remote Config fetch has been activated (and whenever Remote Config is
     * unavailable), so this is always safe to call on any thread.
     */
    fun current(): SettingsDefaults

    /**
     * Fetches and activates the newest values from the server, if available.
     * A no-op (returning immediately) when Remote Config isn't configured.
     * Safe to call at startup; failures are swallowed and leave [current] unchanged.
     */
    suspend fun refresh()
}
