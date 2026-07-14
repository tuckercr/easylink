package com.fangjet.launcher.util

import android.content.Context
import android.os.Build
import androidx.core.os.ConfigurationCompat
import androidx.core.text.util.LocalePreferences
import androidx.core.text.util.LocalePreferences.TemperatureUnit
import com.fangjet.launcher.domain.model.WeatherUnits
import java.util.Locale

/**
 * Resolves [WeatherUnits] for the Open-Meteo API (`celsius` vs `fahrenheit`).
 *
 * On API 34+ uses [LocalePreferences.getTemperatureUnit] to honour the user's
 * explicit regional preference (Settings → General management → Language → Regional).
 * Below API 34 falls back to [WeatherUnits.fromLocale] (US / territories → imperial,
 * everywhere else → metric).
 */
fun Context.resolveWeatherUnits(): WeatherUnits {
    val locale: Locale =
        ConfigurationCompat.getLocales(resources.configuration).get(0) ?: Locale.getDefault()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return when (LocalePreferences.getTemperatureUnit()) {
            TemperatureUnit.CELSIUS -> WeatherUnits.METRIC
            TemperatureUnit.FAHRENHEIT -> WeatherUnits.IMPERIAL
            else -> WeatherUnits.fromLocale(locale)
        }
    }
    return WeatherUnits.fromLocale(locale)
}
