package com.fangjet.launcher.data.apps

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Counts app launches made through the launcher.
 *
 * Because EasyLink *is* the home screen, every app the user opens goes through
 * [com.fangjet.launcher.domain.usecase.LaunchAppUseCase] — so simple local
 * counting gives an accurate "most used" ranking with **no** UsageStats special
 * permission, which would be a hostile settings journey for this audience.
 *
 * Counts are stored per package as `count_<packageName>` in their own DataStore.
 */
@Singleton
class AppUsageTracker @Inject constructor(
    @param:Named("appUsagePrefs") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private const val KEY_PREFIX = "count_"
    }

    /** Emits package → launch count, re-emitting after every recorded launch. */
    val launchCounts: Flow<Map<String, Long>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs
                .asMap()
                .entries
                .filter { it.key.name.startsWith(KEY_PREFIX) }
                .associate { (key, value) ->
                    key.name.removePrefix(KEY_PREFIX) to (value as? Long ?: 0L)
                }
        }

    suspend fun recordLaunch(packageName: String) {
        val key = longPreferencesKey(KEY_PREFIX + packageName)
        dataStore.edit { prefs -> prefs[key] = (prefs[key] ?: 0L) + 1L }
    }
}
