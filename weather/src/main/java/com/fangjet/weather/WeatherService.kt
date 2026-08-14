package com.fangjet.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.fangjet.weather.data.WeatherPreferencesDataSource
import com.fangjet.weather.model.ForecastDay
import com.fangjet.weather.model.ForecastResult
import com.fangjet.weather.model.WeatherInfo
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * Fetches weather data from Open-Meteo. The endpoint and API key come from
 * [WeatherApiConfigProvider] — keyless free tier by default, switchable to the
 * paid keyed endpoint by the consuming app (see [WeatherApiConfig]).
 *
 * ## Location strategy
 *
 * Each fetch attempt goes through up to four stages, short-circuiting as soon
 * as a usable coordinate pair is found:
 *
 * 1. **FusedLocation cache** – instant, zero battery cost. Empty after a reboot.
 * 2. **LocationManager caches** – each provider keeps its own last-known fix.
 *    Populated by other apps (Maps, Camera …) even when FusedLocation's cache
 *    is empty.
 * 3. **Active `requestLocationUpdates`** – actually wakes the location hardware
 *    and waits up to [LOCATION_TIMEOUT_MS] for a fix.
 * 4. **Saved coordinates (DataStore)** – used when all live-location stages fail
 *    (location services off, or just no signal). Weather is fetched normally but
 *    the card shows a "saved location" note.
 *
 * Coordinates from a successful live fetch are persisted after every call so the
 * saved-location fallback stays fresh.
 */
@Singleton
class WeatherService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val weatherPrefs: WeatherPreferencesDataSource,
    private val apiConfig: WeatherApiConfigProvider,
    private val fetchReporter: WeatherFetchReporter,
) {

    companion object {
        private const val TAG = "WeatherService"
        private const val LOCATION_TIMEOUT_MS = 12_000L
        private const val MAX_CACHE_AGE_MS = 30 * 60 * 1_000L // 30 minutes

        // Telemetry endpoint labels — never the URL, which carries coordinates.
        private const val ENDPOINT_CURRENT = "current"
        private const val ENDPOINT_FORECAST = "forecast"
    }

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun fetch(): WeatherInfo =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "fetch() called")
            val units = context.resolveWeatherUnits()
            Log.d(TAG, "Temperature units: $units")

            if (!hasLocationPermission()) {
                Log.d(TAG, "Permission not granted → PermissionNeeded")
                return@withContext WeatherInfo.PermissionNeeded
            }

            val locationDisabled = !isLocationEnabled()
            Log.d(TAG, "Location services enabled: ${!locationDisabled}")

            // Attempt to get a live location (skipped quickly if location is off)
            val liveLocation: Location? = if (locationDisabled) {
                Log.d(TAG, "Location is off — skipping live fetch")
                null
            } else {
                getLocation()
            }

            // Determine the coordinate source and whether we're using cache
            val (lat, lon, city, fromCache) = if (liveLocation != null) {
                // Save fresh coords for future fallback
                val resolvedCity = reverseGeocodeCity(liveLocation.latitude, liveLocation.longitude)
                Log.d(
                    TAG,
                    "Live location: ${liveLocation.latitude}, ${liveLocation.longitude}, city=$resolvedCity",
                )
                weatherPrefs.saveLocation(
                    liveLocation.latitude,
                    liveLocation.longitude,
                    resolvedCity,
                )
                Quad(liveLocation.latitude, liveLocation.longitude, resolvedCity, false)
            } else {
                // Try the DataStore cache
                val cached = weatherPrefs.getCachedLocation()
                if (cached != null) {
                    Log.d(
                        TAG,
                        "Using saved location: ${cached.latitude}, ${cached.longitude}, city=${cached.city}",
                    )
                    Quad(cached.latitude, cached.longitude, cached.city, true)
                } else {
                    // No location at all — return the right error state
                    return@withContext if (locationDisabled) {
                        Log.w(TAG, "Location disabled and no cached location → LocationDisabled")
                        WeatherInfo.LocationDisabled
                    } else {
                        Log.w(TAG, "No location fix and no cached location → Unavailable")
                        WeatherInfo.Unavailable("No location fix yet")
                    }
                }
            }

            return@withContext try {
                val url = WeatherUrls.current(apiConfig.current(), lat, lon, units)
                Log.d(TAG, "Fetching current weather (fromCache=$fromCache)")
                val json = httpGet(url, ENDPOINT_CURRENT) ?: run {
                    Log.w(TAG, "No response from weather API — trying cached weather")
                    return@withContext cachedWeatherFallback(city, fromCache)
                }
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m")
                val code = current.getInt("weather_code")
                val description = codeToDescription(code)
                val emoji = codeToEmoji(code)
                Log.d(
                    TAG,
                    "Weather: temp=$temp ${units.symbol}, code=$code, city=$city, fromCache=$fromCache",
                )

                // Persist so we can show it the next time the network is unavailable
                weatherPrefs.saveWeather(temp, description, emoji, units.isFahrenheit)

                WeatherInfo.Available(
                    temperature = temp,
                    description = description,
                    emoji = emoji,
                    isFahrenheit = units.isFahrenheit,
                    city = city,
                    usingCachedLocation = fromCache,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Weather fetch failed", e)
                cachedWeatherFallback(city, fromCache)
            }
        }

    suspend fun fetchForecast(): ForecastResult =
        withContext(Dispatchers.IO) {
            val units = context.resolveWeatherUnits()
            if (!hasLocationPermission()) return@withContext ForecastResult.PermissionNeeded

            val locationDisabled = !isLocationEnabled()
            val liveLocation = if (locationDisabled) null else getLocation()
            val fromCache: Boolean

            val (lat, lon, city) = if (liveLocation != null) {
                fromCache = false
                val resolvedCity = reverseGeocodeCity(liveLocation.latitude, liveLocation.longitude)
                weatherPrefs.saveLocation(
                    liveLocation.latitude,
                    liveLocation.longitude,
                    resolvedCity,
                )
                Triple(liveLocation.latitude, liveLocation.longitude, resolvedCity)
            } else {
                fromCache = true
                val cached = weatherPrefs.getCachedLocation()
                    ?: return@withContext if (locationDisabled) {
                        ForecastResult.LocationDisabled
                    } else {
                        ForecastResult.Unavailable(
                            "Could not get a location fix. Make sure location services are on and try again.",
                        )
                    }
                Triple(cached.latitude, cached.longitude, cached.city)
            }

            return@withContext try {
                val url = WeatherUrls.forecast(apiConfig.current(), lat, lon, units)
                val json = httpGet(url, ENDPOINT_FORECAST) ?: return@withContext ForecastResult.Unavailable(
                    "No internet connection. Check your network settings and try again.",
                )

                val daily = json.getJSONObject("daily")
                val times = daily.getJSONArray("time")
                val maxT = daily.getJSONArray("temperature_2m_max")
                val minT = daily.getJSONArray("temperature_2m_min")
                val codes = daily.getJSONArray("weather_code")
                val precip = daily.getJSONArray("precipitation_probability_max")

                val days = (0 until times.length()).map { i ->
                    val code = codes.getInt(i)
                    ForecastDay(
                        date = times.getString(i),
                        tempMax = maxT.getDouble(i),
                        tempMin = minT.getDouble(i),
                        emoji = codeToEmoji(code),
                        description = codeToDescription(code),
                        precipitationChance = if (precip.isNull(i)) 0 else precip.getInt(i),
                    )
                }
                ForecastResult.Success(city = city, days = days, usingCachedLocation = fromCache)
            } catch (e: Exception) {
                Log.e(TAG, "fetchForecast network error", e)
                ForecastResult.Unavailable(
                    "No internet connection. Check your network settings and try again.",
                )
            }
        }

    // ── Location helpers ──────────────────────────────────────────────────────

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // isLocationEnabled requires API 28; isProviderEnabled is available since API 1.
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Three-stage live location fetch with a hard timeout.
     *
     * Stage 1 — FusedLocation cache (instant)
     * Stage 2 — LocationManager provider caches (populated by other apps)
     * Stage 3 — requestLocationUpdates (wakes hardware, waits up to timeout)
     */
    private suspend fun getLocation(): Location? =
        withTimeoutOrNull(LOCATION_TIMEOUT_MS.milliseconds) {
            getFusedLastLocation()
                ?: getLocationManagerCached()
                ?: requestFreshLocation()
        }

    @Suppress("MissingPermission")
    private suspend fun getFusedLastLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                LocationServices
                    .getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener { loc ->
                        Log.d(
                            TAG,
                            if (loc != null) "Stage 1 hit: ${loc.latitude}, ${loc.longitude}" else "Stage 1 miss",
                        )
                        if (cont.isActive) cont.resume(loc)
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Stage 1 failed", e)
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Stage 1 exception", e)
                if (cont.isActive) cont.resume(null)
            }
        }

    @Suppress("MissingPermission")
    private fun getLocationManagerCached(): Location? =
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val now = System.currentTimeMillis()
            val best = lm
                .getProviders(true)
                .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
                .filter { loc -> (now - loc.time) <= MAX_CACHE_AGE_MS }
                .maxByOrNull { it.time }
            Log.d(
                TAG,
                if (best !=
                    null
                ) {
                    "Stage 2 hit: ${best.latitude}, ${best.longitude}, age=${(now - best.time) / 60_000}min"
                } else {
                    "Stage 2 miss"
                },
            )
            best
        } catch (e: Exception) {
            Log.e(TAG, "Stage 2 exception", e)
            null
        }

    @Suppress("MissingPermission")
    private suspend fun requestFreshLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                Log.d(TAG, "Stage 3: starting requestLocationUpdates…")
                val client = LocationServices.getFusedLocationProviderClient(context)
                val request =
                    LocationRequest
                        .Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5_000L)
                        .setMaxUpdates(1)
                        .setWaitForAccurateLocation(false)
                        .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val loc = result.lastLocation
                        Log.d(
                            TAG,
                            if (loc != null) "Stage 3 hit: ${loc.latitude}, ${loc.longitude}" else "Stage 3: null lastLocation",
                        )
                        client.removeLocationUpdates(this)
                        if (cont.isActive) cont.resume(loc)
                    }
                }

                cont.invokeOnCancellation { client.removeLocationUpdates(callback) }
                client
                    .requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Stage 3 failed to register", e)
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Stage 3 exception", e)
                if (cont.isActive) cont.resume(null)
            }
        }

    // ── Cached-weather fallback ───────────────────────────────────────────────

    /**
     * Returns the last successfully fetched weather as [WeatherInfo.Available] with
     * [WeatherInfo.Available.usingCachedWeather] = true, or [WeatherInfo.Unavailable]
     * if no weather has ever been cached (i.e. first launch in airplane mode).
     */
    private suspend fun cachedWeatherFallback(
        city: String?,
        usingCachedLocation: Boolean,
    ): WeatherInfo {
        val cached = weatherPrefs.getCachedWeather()
        return if (cached != null) {
            Log.d(TAG, "Network unavailable — returning cached weather from ${cached.fetchedAt}")
            WeatherInfo.Available(
                temperature = cached.temperature,
                description = cached.description,
                emoji = cached.emoji,
                isFahrenheit = cached.isFahrenheit,
                city = city,
                usingCachedLocation = usingCachedLocation,
                usingCachedWeather = true,
                weatherCachedAt = cached.fetchedAt,
            )
        } else {
            Log.w(TAG, "Network unavailable and no cached weather — Unavailable")
            WeatherInfo.Unavailable("No internet connection. Connect to a network and try again.")
        }
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private fun httpGet(
        urlStr: String,
        endpoint: String,
    ): JSONObject? {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            val code = conn.responseCode
            Log.d(TAG, "HTTP $code ($endpoint)")
            if (code != 200) {
                Log.e(TAG, "Non-200: $code ($endpoint)")
                // Surface API-side failures (rate limiting especially) to the
                // consuming app's telemetry; plain connectivity problems throw
                // before a status code exists and stay local.
                fetchReporter.onHttpError(code, endpoint)
                return null
            }
            JSONObject(conn.inputStream.bufferedReader().readText())
        } catch (e: Exception) {
            Log.e(TAG, "httpGet failed", e)
            null
        }
    }

    // ── Reverse geocoding ─────────────────────────────────────────────────────

    private fun reverseGeocodeCity(
        lat: Double,
        lon: Double,
    ): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault())
                .getFromLocation(lat, lon, 1)
                ?.firstOrNull()
                ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        } catch (_: Exception) {
            null
        }
    }

    // ── WMO weather-code tables ───────────────────────────────────────────────

    internal fun codeToEmoji(code: Int): String =
        when (code) {
            0 -> "☀️"
            1 -> "🌤️"
            2 -> "⛅"
            3 -> "☁️"
            45, 48 -> "🌫️"
            in 51..57 -> "🌦️"
            in 61..67 -> "🌧️"
            in 71..77 -> "🌨️"
            in 80..82 -> "🌦️"
            85, 86 -> "🌨️"
            95 -> "⛈️"
            in 96..99 -> "⛈️"
            else -> "🌡️"
        }

    internal fun codeToDescription(code: Int): String =
        when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            in 51..57 -> "Drizzle"
            in 61..67 -> "Rainy"
            in 71..77 -> "Snowing"
            in 80..82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            in 96..99 -> "Thunderstorm"
            else -> "Unknown"
        }
}

/** Tiny data class to destructure four values from a single expression. */
private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
