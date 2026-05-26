package com.tuckercr.ezlauncher.data.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.tuckercr.ezlauncher.domain.model.WeatherInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Fetches current weather from Open-Meteo (open-source, no API key needed).
 *
 * Flow:
 *  1. Check ACCESS_COARSE_LOCATION permission → return [WeatherInfo.PermissionNeeded] if absent
 *  2. Get last-known device location via FusedLocationProviderClient
 *  3. Call api.open-meteo.com with lat/lon
 *  4. Parse WMO weather code → emoji + description
 */
@Singleton
class WeatherService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun fetch(): WeatherInfo = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext WeatherInfo.PermissionNeeded

        val location = getLastLocation()
            ?: return@withContext WeatherInfo.Unavailable("No location fix yet")

        return@withContext try {
            val urlStr = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${location.latitude}" +
                "&longitude=${location.longitude}" +
                "&current=temperature_2m,weather_code" +
                "&temperature_unit=celsius"

            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 6_000
            conn.readTimeout    = 6_000

            if (conn.responseCode != 200) {
                return@withContext WeatherInfo.Unavailable("HTTP ${conn.responseCode}")
            }

            val json    = JSONObject(conn.inputStream.bufferedReader().readText())
            val current = json.getJSONObject("current")
            val temp    = current.getDouble("temperature_2m")
            val code    = current.getInt("weather_code")

            WeatherInfo.Available(
                temperatureCelsius = temp,
                description        = codeToDescription(code),
                emoji              = codeToEmoji(code),
            )
        } catch (e: Exception) {
            WeatherInfo.Unavailable(e.message ?: "Network error")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private suspend fun getLastLocation(): Location? = suspendCancellableCoroutine { cont ->
        try {
            val task = LocationServices.getFusedLocationProviderClient(context).lastLocation
            task.addOnSuccessListener  { loc -> if (cont.isActive) cont.resume(loc) }
            task.addOnFailureListener  {       if (cont.isActive) cont.resume(null) }
            task.addOnCanceledListener {       if (cont.isActive) cont.cancel() }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    // ── WMO weather-code tables ───────────────────────────────────────────────

    private fun codeToEmoji(code: Int): String = when (code) {
        0            -> "☀️"
        1            -> "🌤️"
        2            -> "⛅"
        3            -> "☁️"
        45, 48       -> "🌫️"
        in 51..57    -> "🌦️"
        in 61..67    -> "🌧️"
        in 71..77    -> "🌨️"
        in 80..82    -> "🌦️"
        85, 86       -> "🌨️"
        95           -> "⛈️"
        in 96..99    -> "⛈️"
        else         -> "🌡️"
    }

    private fun codeToDescription(code: Int): String = when (code) {
        0            -> "Clear sky"
        1            -> "Mainly clear"
        2            -> "Partly cloudy"
        3            -> "Overcast"
        45, 48       -> "Foggy"
        in 51..57    -> "Drizzle"
        in 61..67    -> "Rainy"
        in 71..77    -> "Snowing"
        in 80..82    -> "Rain showers"
        85, 86       -> "Snow showers"
        95           -> "Thunderstorm"
        in 96..99    -> "Thunderstorm"
        else         -> "Unknown"
    }
}
