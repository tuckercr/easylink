package com.fangjet.weather.model

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * One day's weather forecast as returned by the Open-Meteo daily endpoint.
 *
 * [emoji] and [description] are pre-resolved from the WMO weather code by
 * [WeatherService] so the domain model stays free of code-table logic.
 */
data class ForecastDay(
    /** ISO date string, e.g. "2024-06-01". */
    val date: String,
    /** High temperature in whichever unit was requested (Celsius or Fahrenheit). */
    val tempMax: Double,
    /** Low temperature in whichever unit was requested (Celsius or Fahrenheit). */
    val tempMin: Double,
    val emoji: String,
    val description: String,
    /** 0–100 % chance of precipitation. */
    val precipitationChance: Int,
) {
    val displayMax: String get() = "${tempMax.toInt()}°"
    val displayMin: String get() = "${tempMin.toInt()}°"

    /** "Today", "Tomorrow", or abbreviated weekday ("Mon", "Tue", …). */
    val dayLabel: String
        get() = try {
            val d = LocalDate.parse(date)
            val today = LocalDate.now()
            when (d) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                else ->
                    d.dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
        } catch (_: Exception) {
            date
        }
}
