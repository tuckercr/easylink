package com.fangjet.weather.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
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
    @param:Named("weatherPrefs") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        // Location keys
        private val KEY_LAT = doublePreferencesKey("cached_lat")
        private val KEY_LON = doublePreferencesKey("cached_lon")
        private val KEY_CITY = stringPreferencesKey("cached_city")

        // Weather keys
        private val KEY_WEATHER_TEMP = doublePreferencesKey("cached_weather_temp")
        private val KEY_WEATHER_DESC = stringPreferencesKey("cached_weather_desc")
        private val KEY_WEATHER_EMOJI = stringPreferencesKey("cached_weather_emoji")
        private val KEY_WEATHER_AT = longPreferencesKey("cached_weather_at")
        private val KEY_WEATHER_IS_FAHRENHEIT = booleanPreferencesKey("cached_weather_is_fahrenheit")
    }

    data class CachedLocation(
        val latitude: Double,
        val longitude: Double,
        val city: String?,
    )

    data class CachedWeather(
        /** Temperature value in the unit that was active when the data was fetched. */
        val temperature: Double,
        val description: String,
        val emoji: String,
        /** True when [temperature] is in Fahrenheit; false for Celsius. */
        val isFahrenheit: Boolean,
        /** Epoch millis when this weather was originally fetched from the network. */
        val fetchedAt: Long,
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

    /** Returns the last successfully fetched weather, or null if none has been saved yet. */
    suspend fun getCachedWeather(): CachedWeather? {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        val temp = prefs[KEY_WEATHER_TEMP] ?: return null
        val desc = prefs[KEY_WEATHER_DESC] ?: return null
        val emoji = prefs[KEY_WEATHER_EMOJI] ?: return null
        val at = prefs[KEY_WEATHER_AT] ?: return null
        val isFahrenheit = prefs[KEY_WEATHER_IS_FAHRENHEIT] ?: false
        return CachedWeather(temp, desc, emoji, isFahrenheit, at)
    }

    /** Persists weather data after a successful network fetch. */
    suspend fun saveWeather(
        temperature: Double,
        description: String,
        emoji: String,
        isFahrenheit: Boolean,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_WEATHER_TEMP] = temperature
            prefs[KEY_WEATHER_DESC] = description
            prefs[KEY_WEATHER_EMOJI] = emoji
            prefs[KEY_WEATHER_AT] = System.currentTimeMillis()
            prefs[KEY_WEATHER_IS_FAHRENHEIT] = isFahrenheit
        }
    }
}
