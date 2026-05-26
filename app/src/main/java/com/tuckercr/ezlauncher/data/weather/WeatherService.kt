package com.tuckercr.ezlauncher.data.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.tuckercr.ezlauncher.domain.model.ForecastDay
import com.tuckercr.ezlauncher.domain.model.WeatherInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Fetches weather data from Open-Meteo (open-source, no API key required).
 *
 * Provides two entry points:
 *  - [fetch]          → current conditions + city name (for the home card)
 *  - [fetchForecast]  → 7-day daily forecast (for the forecast screen)
 *
 * City name is resolved via Android's [Geocoder] (reverse geocoding).
 */
@Singleton
class WeatherService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns current weather including reverse-geocoded city name. */
    suspend fun fetch(): WeatherInfo = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext WeatherInfo.PermissionNeeded

        val location = getLastLocation()
            ?: return@withContext WeatherInfo.Unavailable("No location fix yet")

        val city = reverseGeocodeCity(location.latitude, location.longitude)

        return@withContext try {
            val url = buildCurrentUrl(location.latitude, location.longitude)
            val json    = httpGet(url) ?: return@withContext WeatherInfo.Unavailable("No response")
            val current = json.getJSONObject("current")
            val temp    = current.getDouble("temperature_2m")
            val code    = current.getInt("weather_code")

            WeatherInfo.Available(
                temperatureCelsius = temp,
                description        = codeToDescription(code),
                emoji              = codeToEmoji(code),
                city               = city,
            )
        } catch (e: Exception) {
            WeatherInfo.Unavailable(e.message ?: "Network error")
        }
    }

    /**
     * Returns a 7-day daily forecast plus the city name.
     * Returns an empty list if location permission is denied or no fix is available.
     */
    suspend fun fetchForecast(): Pair<String?, List<ForecastDay>> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext Pair(null, emptyList())

        val location = getLastLocation()
            ?: return@withContext Pair(null, emptyList())

        val city = reverseGeocodeCity(location.latitude, location.longitude)

        return@withContext try {
            val url  = buildForecastUrl(location.latitude, location.longitude)
            val json = httpGet(url) ?: return@withContext Pair(city, emptyList())

            val daily   = json.getJSONObject("daily")
            val times   = daily.getJSONArray("time")
            val maxT    = daily.getJSONArray("temperature_2m_max")
            val minT    = daily.getJSONArray("temperature_2m_min")
            val codes   = daily.getJSONArray("weather_code")
            val precip  = daily.getJSONArray("precipitation_probability_max")

            val days = (0 until times.length()).map { i ->
                val code = codes.getInt(i)
                ForecastDay(
                    date                = times.getString(i),
                    tempMaxCelsius      = maxT.getDouble(i),
                    tempMinCelsius      = minT.getDouble(i),
                    emoji               = codeToEmoji(code),
                    description         = codeToDescription(code),
                    precipitationChance = if (precip.isNull(i)) 0 else precip.getInt(i),
                )
            }

            Pair(city, days)
        } catch (_: Exception) {
            Pair(city, emptyList())
        }
    }

    // ── URL builders ──────────────────────────────────────────────────────────

    private fun buildCurrentUrl(lat: Double, lon: Double) =
        "https://api.open-meteo.com/v1/forecast" +
        "?latitude=$lat&longitude=$lon" +
        "&current=temperature_2m,weather_code" +
        "&temperature_unit=celsius"

    private fun buildForecastUrl(lat: Double, lon: Double) =
        "https://api.open-meteo.com/v1/forecast" +
        "?latitude=$lat&longitude=$lon" +
        "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max" +
        "&forecast_days=7" +
        "&temperature_unit=celsius" +
        "&timezone=auto"

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private fun httpGet(urlStr: String): JSONObject? {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout    = 6_000
        if (conn.responseCode != 200) return null
        return JSONObject(conn.inputStream.bufferedReader().readText())
    }

    // ── Location ──────────────────────────────────────────────────────────────

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
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    // ── Reverse geocoding ─────────────────────────────────────────────────────

    private fun reverseGeocodeCity(lat: Double, lon: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            @Suppress("DEPRECATION")
            val addresses = Geocoder(context, Locale.getDefault())
                .getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.let { addr ->
                addr.locality ?: addr.subAdminArea ?: addr.adminArea
            }
        } catch (_: Exception) {
            null
        }
    }

    // ── WMO weather-code tables ───────────────────────────────────────────────

    internal fun codeToEmoji(code: Int): String = when (code) {
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

    internal fun codeToDescription(code: Int): String = when (code) {
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
