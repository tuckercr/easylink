package com.fangjet.weather

import com.fangjet.weather.model.WeatherUnits

/**
 * Where weather requests go.
 *
 * The default targets Open-Meteo's free, keyless endpoint (licensed for
 * non-commercial use). Their paid plan serves the *same API* from a keyed
 * endpoint (`https://customer-api.open-meteo.com` + `apikey` parameter), so
 * swapping [baseUrl] and [apiKey] is a complete migration — no response-format
 * changes. The launcher backs this with Remote Config precisely so that swap
 * can happen from the Firebase console without shipping an app update.
 */
data class WeatherApiConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    /** Appended as `&apikey=` when non-blank (Open-Meteo's paid endpoints). */
    val apiKey: String = "",
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.open-meteo.com"
    }
}

/**
 * Supplies the current [WeatherApiConfig] at fetch time (not construction time),
 * so a config change reaches the next request without recreating the service.
 * The consuming app decides where values come from; this module has no opinion.
 */
fun interface WeatherApiConfigProvider {
    fun current(): WeatherApiConfig
}

/**
 * Telemetry hook for weather API failures.
 *
 * [endpoint] is a short label ("current", "forecast") — deliberately not the
 * URL, which would carry the user's coordinates into logging backends.
 * The consuming app decides what to do with reports (the launcher records
 * Crashlytics non-fatals so rate limiting shows up in a dashboard).
 */
fun interface WeatherFetchReporter {
    fun onHttpError(
        httpCode: Int,
        endpoint: String,
    )
}

/** Builds Open-Meteo request URLs. Pure — unit-tested on the JVM. */
internal object WeatherUrls {
    fun current(
        config: WeatherApiConfig,
        lat: Double,
        lon: Double,
        units: WeatherUnits,
    ): String =
        "${config.baseUrl.trimEnd('/')}/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code" +
            "&temperature_unit=${units.apiParam}" +
            apiKeyParam(config)

    fun forecast(
        config: WeatherApiConfig,
        lat: Double,
        lon: Double,
        units: WeatherUnits,
    ): String =
        "${config.baseUrl.trimEnd('/')}/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max" +
            "&forecast_days=7" +
            "&temperature_unit=${units.apiParam}" +
            "&timezone=auto" +
            apiKeyParam(config)

    private fun apiKeyParam(config: WeatherApiConfig): String = if (config.apiKey.isBlank()) "" else "&apikey=${config.apiKey}"
}
