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

    /** Weather data successfully retrieved. */
    data class Available(
        val temperatureCelsius: Double,
        val description: String,
        val emoji: String,
    ) : WeatherInfo() {
        val displayTemp: String get() = "${temperatureCelsius.toInt()}°C"
    }

    /** Could not fetch weather (no location fix, network error, etc.). */
    data class Unavailable(val reason: String = "") : WeatherInfo()
}
