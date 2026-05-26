package com.tuckercr.ezlauncher.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Persists the last successfully resolved location so the weather widget
 * can continue showing data when location services are temporarily disabled.
 */
@Singleton
class WeatherPreferencesDataSource @Inject constructor(
    @Named("weatherPrefs") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_LAT = doublePreferencesKey("cached_lat")
        private val KEY_LON = doublePreferencesKey("cached_lon")
        private val KEY_CITY = stringPreferencesKey("cached_city")
    }

    data class CachedLocation(
        val latitude: Double,
        val longitude: Double,
        val city: String?,
    )

    /** Returns the last saved location, or null if none has been saved yet. */
    suspend fun getCachedLocation(): CachedLocation? {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        val lat = prefs[KEY_LAT] ?: return null
        val lon = prefs[KEY_LON] ?: return null
        return CachedLocation(lat, lon, prefs[KEY_CITY])
    }

    /** Persists a location after a successful weather fetch. */
    suspend fun saveLocation(
        latitude: Double,
        longitude: Double,
        city: String?,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_LAT] = latitude
            prefs[KEY_LON] = longitude
            if (city != null) prefs[KEY_CITY] = city else prefs.remove(KEY_CITY)
        }
    }
}
