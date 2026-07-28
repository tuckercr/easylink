package com.fangjet.weather.model

/**
 * Typed result from [WeatherService.fetchForecast].
 *
 * Using a sealed class instead of a raw Pair lets the ViewModel and UI map each
 * failure reason to a specific, actionable message rather than a generic error string.
 */
sealed class ForecastResult {

    /** Forecast data retrieved successfully. */
    data class Success(
        val city: String?,
        val days: List<ForecastDay>,
        /** True if the forecast was fetched using a saved location (live location unavailable). */
        val usingCachedLocation: Boolean = false,
    ) : ForecastResult()

    /** Location permission has not been granted. */
    data object PermissionNeeded : ForecastResult()

    /**
     * Location services are turned off at the device level.
     * There is also no saved location to fall back on.
     */
    data object LocationDisabled : ForecastResult()

    /**
     * Forecast could not be loaded. [message] describes the specific reason
     * (e.g. no network, no location fix) so the UI can show actionable text.
     */
    data class Unavailable(
        val message: String,
    ) : ForecastResult()
}
