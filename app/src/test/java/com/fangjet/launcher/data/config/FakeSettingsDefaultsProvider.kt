package com.fangjet.launcher.data.config

import com.fangjet.shared.config.SettingsDefaults

/**
 * Test double that returns a fixed [SettingsDefaults] with no Firebase involved.
 * Defaults to [SettingsDefaults.HARDCODED] so existing tests see the same
 * behaviour as before the provider was introduced.
 */
class FakeSettingsDefaultsProvider(
    private val defaults: SettingsDefaults = SettingsDefaults.HARDCODED,
) : SettingsDefaultsProvider {
    var refreshCount = 0
        private set

    override fun current(): SettingsDefaults = defaults

    override suspend fun refresh() {
        refreshCount++
    }
}
