package com.tuckercr.ezlauncher.domain.model

import java.util.Locale

/**
 * Temperature unit preference — resolved from device locale / system settings.
 *
 * [apiParam] is the value passed to the Open-Meteo `temperature_unit` query parameter.
 */
enum class WeatherUnits {
    METRIC, // Celsius
    IMPERIAL, // Fahrenheit
    ;

    /** Open-Meteo query parameter value. */
    val apiParam: String
        get() = when (this) {
            METRIC -> "celsius"
            IMPERIAL -> "fahrenheit"
        }

    /** Display symbol appended to numeric temperature values. */
    val symbol: String
        get() = when (this) {
            METRIC -> "°C"
            IMPERIAL -> "°F"
        }

    val isFahrenheit: Boolean get() = this == IMPERIAL

    companion object {
        /**
         * Fallback when the regional temperature preference is unavailable (pre-API 34).
         * United States and its territories → imperial; everywhere else → metric.
         */
        fun fromLocale(locale: Locale): WeatherUnits {
            val country = locale.country.uppercase(Locale.ROOT)
            val imperialCountries = setOf("US", "PR", "GU", "VI", "AS", "MP", "UM")
            return if (country in imperialCountries) IMPERIAL else METRIC
        }
    }
}
