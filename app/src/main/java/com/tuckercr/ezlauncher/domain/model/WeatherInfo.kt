package com.tuckercr.ezlauncher.domain.model

/**
 * Snapshot of current weather conditions.
 *
 * Fetched from Open-Meteo (no API key required).
 * Location comes from FusedLocationProviderClient.
 */
sealed class WeatherInfo {
    /** Initial state — fetch not yet attempted. */
    data object Loading : WeatherInfo()

    /** Location permission not granted. User must tap to grant it. */
    data object PermissionNeeded : WeatherInfo()

    /**
     * Location services are turned off at the device level (not a permission issue).
     * Only shown when there are no cached coordinates to fall back on.
     * Tapping should open system location settings.
     */
    data object LocationDisabled : WeatherInfo()

    /** Weather data successfully retrieved. */
    data class Available(
        /** Temperature value in whichever unit was requested (Celsius or Fahrenheit). */
        val temperature: Double,
        val description: String,
        val emoji: String,
        /** True when [temperature] is in Fahrenheit; false for Celsius. */
        val isFahrenheit: Boolean = false,
        /** Reverse-geocoded city name, or null if geocoding is unavailable. */
        val city: String? = null,
        /**
         * True when weather was fetched using a cached location because
         * live location is currently off or unavailable.
         */
        val usingCachedLocation: Boolean = false,
        /**
         * True when the weather data itself is from a previous fetch because
         * the device is currently offline.
         */
        val usingCachedWeather: Boolean = false,
        /** Epoch millis when the weather data was last successfully fetched from the network. */
        val weatherCachedAt: Long = 0L,
    ) : WeatherInfo() {
        val displayTemp: String
            get() = "${temperature.toInt()}${if (isFahrenheit) "°F" else "°C"}"
    }

    /** Could not fetch weather (no location fix, network error, etc.). */
    data class Unavailable(
        val reason: String = "",
    ) : WeatherInfo()
}
