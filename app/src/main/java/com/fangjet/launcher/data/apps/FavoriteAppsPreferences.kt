package com.fangjet.launcher.data.apps

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** How the My Apps row picks which apps to show. */
enum class FavoriteAppsMode {
    /** Most-launched apps, ranked by [AppUsageTracker]. */
    AUTOMATIC,

    /** Exactly the apps the user (or a caregiver, later) picked. */
    CUSTOM,
}

/**
 * Settings for the scrollable My Apps row on the Home screen.
 *
 * The custom list is stored as a newline-joined string of package names —
 * package names cannot contain newlines, so the encoding is unambiguous.
 */
@Singleton
class FavoriteAppsPreferences @Inject constructor(
    @param:Named("favoriteAppsPrefs") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_ROW_ENABLED = booleanPreferencesKey("row_enabled")
        private val KEY_MODE = stringPreferencesKey("mode")
        private val KEY_CUSTOM_LIST = stringPreferencesKey("custom_packages")
        private val KEY_BADGES_ENABLED = booleanPreferencesKey("badges_enabled")

        /**
         * Absolute storage sanity cap. The *effective* row cap is
         * `SettingsDefaults.favoriteAppsMaxCount` (Remote Config-tunable);
         * this only guards the persisted list against runaway growth.
         */
        const val HARD_CAP = 30
    }

    /** Whether the row shows on Home at all. Default ON — it's the headline feature. */
    val rowEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_ROW_ENABLED] ?: true }

    val mode: Flow<FavoriteAppsMode> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            FavoriteAppsMode.entries.firstOrNull { it.name == prefs[KEY_MODE] }
                ?: FavoriteAppsMode.AUTOMATIC
        }

    /** Ordered custom selection; empty when the user never picked any. */
    val customPackages: Flow<List<String>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[KEY_CUSTOM_LIST]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
        }

    /** Whether notification dots show on the row. Default OFF (needs a system grant). */
    val badgesEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_BADGES_ENABLED] ?: false }

    suspend fun setRowEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ROW_ENABLED] = enabled }
    }

    suspend fun setMode(mode: FavoriteAppsMode) {
        dataStore.edit { it[KEY_MODE] = mode.name }
    }

    suspend fun setCustomPackages(packages: List<String>) {
        dataStore.edit { it[KEY_CUSTOM_LIST] = packages.take(HARD_CAP).joinToString("\n") }
    }

    suspend fun setBadgesEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BADGES_ENABLED] = enabled }
    }
}
