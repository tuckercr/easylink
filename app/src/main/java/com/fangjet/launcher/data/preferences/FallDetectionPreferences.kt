package com.fangjet.launcher.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fangjet.launcher.domain.model.FallSensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FallDetectionPreferences @Inject constructor(
    @param:Named("fallPrefs") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("fall_detection_enabled")
        private val KEY_SENSITIVITY = stringPreferencesKey("fall_sensitivity")
    }

    val isEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_ENABLED] ?: false }

    val sensitivity: Flow<FallSensitivity> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val name = prefs[KEY_SENSITIVITY] ?: FallSensitivity.MEDIUM.name
            FallSensitivity.entries.firstOrNull { it.name == name } ?: FallSensitivity.MEDIUM
        }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setSensitivity(sensitivity: FallSensitivity) {
        dataStore.edit { it[KEY_SENSITIVITY] = sensitivity.name }
    }
}
