package com.fangjet.launcher.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.fangjet.launcher.data.config.FeatureFlags
import com.fangjet.launcher.data.config.SettingsDefaultsProvider
import com.fangjet.launcher.domain.model.HomeButton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Persists which Home screen buttons the user has enabled.
 *
 * Storage uses two sets:
 * - DISABLED_BUTTONS: buttons with defaultEnabled=true that the user turned off
 * - ENABLED_OPTIONAL_BUTTONS: buttons with defaultEnabled=false that the user turned on
 *
 * This means new opt-in buttons (defaultEnabled=false) stay hidden on upgrade
 * until the user explicitly enables them, while existing on-by-default buttons
 * are unaffected.
 */
@Singleton
class HomePreferencesDataSource @Inject constructor(
    @Named("homePrefs") private val dataStore: DataStore<Preferences>,
    private val defaults: SettingsDefaultsProvider,
    private val featureFlags: FeatureFlags,
) {
    companion object {
        private val DISABLED_BUTTONS = stringSetPreferencesKey("disabled_home_buttons")
        private val ENABLED_OPTIONAL_BUTTONS =
            stringSetPreferencesKey("enabled_optional_home_buttons")
        private val VOICE_BUTTON_ENABLED = booleanPreferencesKey("voice_button_enabled")

        // Default ON — SOS is a core safety feature that should be visible by default.
        private val SOS_BUTTON_ENABLED = booleanPreferencesKey("sos_button_enabled")
        private val HIGH_CONTRAST_ENABLED = booleanPreferencesKey("high_contrast_enabled")
    }

    /** Emits the current set of enabled buttons whenever it changes. */
    val enabledButtons: Flow<Set<HomeButton>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val disabled = prefs[DISABLED_BUTTONS] ?: emptySet()
            val enabledOptional = prefs[ENABLED_OPTIONAL_BUTTONS] ?: emptySet()
            HomeButton.entries
                .filter { button ->
                    if (button.defaultEnabled) {
                        button.name !in disabled
                    } else {
                        button.name in enabledOptional
                    }
                }.toSet()
        }

    /**
     * Whether the full-width voice command button is shown on the Home screen.
     * The Remote Config kill switch ([SettingsDefaults.voiceCommandsFeatureEnabled])
     * overrides everything — including an explicit user preference — so the
     * feature can be withdrawn server-side if it proves problematic.
     */
    val voiceButtonEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            defaults.current().voiceCommandsFeatureEnabled &&
                (prefs[VOICE_BUTTON_ENABLED] ?: defaults.current().voiceButtonVisible)
        }

    suspend fun setVoiceButtonEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[VOICE_BUTTON_ENABLED] = enabled }
    }

    /**
     * Whether the full-width SOS button is shown on the Home screen.
     * Always false in the standard flavor — SEND_SMS is not in its manifest,
     * so neither stored preference nor Remote Config may surface the button.
     */
    val sosButtonEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            featureFlags.safetyFeatures &&
                (prefs[SOS_BUTTON_ENABLED] ?: defaults.current().sosButtonVisible)
        }

    suspend fun setSosButtonEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SOS_BUTTON_ENABLED] = enabled }
    }

    /**
     * Whether the high-contrast theme is active. The Remote Config kill switch
     * overrides everything, including a stored preference — the feature is
     * parked and defaults to hidden.
     */
    val highContrastEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            defaults.current().highContrastFeatureEnabled &&
                (prefs[HIGH_CONTRAST_ENABLED] ?: defaults.current().highContrast)
        }

    suspend fun setHighContrastEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[HIGH_CONTRAST_ENABLED] = enabled }
    }

    /** Persist the enabled/disabled state for a single button. */
    suspend fun setButtonEnabled(
        button: HomeButton,
        enabled: Boolean,
    ) {
        dataStore.edit { prefs ->
            if (button.defaultEnabled) {
                val current = (prefs[DISABLED_BUTTONS] ?: emptySet()).toMutableSet()
                if (enabled) current.remove(button.name) else current.add(button.name)
                prefs[DISABLED_BUTTONS] = current
            } else {
                val current = (prefs[ENABLED_OPTIONAL_BUTTONS] ?: emptySet()).toMutableSet()
                if (enabled) current.add(button.name) else current.remove(button.name)
                prefs[ENABLED_OPTIONAL_BUTTONS] = current
            }
        }
    }

    /** Resets all home button preferences to their default values. */
    suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            prefs.remove(DISABLED_BUTTONS)
            prefs.remove(ENABLED_OPTIONAL_BUTTONS)
            prefs.remove(VOICE_BUTTON_ENABLED)
            prefs.remove(SOS_BUTTON_ENABLED)
            prefs.remove(HIGH_CONTRAST_ENABLED)
        }
    }
}
