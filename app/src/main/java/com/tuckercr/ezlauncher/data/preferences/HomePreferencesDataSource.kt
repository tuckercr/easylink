package com.tuckercr.ezlauncher.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tuckercr.ezlauncher.domain.model.HomeButton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Persists which Home screen buttons the user has enabled.
 *
 * We store the SET OF DISABLED button names (so new buttons default to enabled
 * without any migration needed).
 */
@Singleton
class HomePreferencesDataSource @Inject constructor(
    @Named("homePrefs") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val DISABLED_BUTTONS = stringSetPreferencesKey("disabled_home_buttons")
    }

    /** Emits the current set of enabled buttons whenever it changes. */
    val enabledButtons: Flow<Set<HomeButton>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val disabled = prefs[DISABLED_BUTTONS] ?: emptySet()
            HomeButton.entries.filter { it.name !in disabled }.toSet()
        }

    /** Persist the enabled/disabled state for a single button. */
    suspend fun setButtonEnabled(button: HomeButton, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = (prefs[DISABLED_BUTTONS] ?: emptySet()).toMutableSet()
            if (enabled) current.remove(button.name) else current.add(button.name)
            prefs[DISABLED_BUTTONS] = current
        }
    }
}
