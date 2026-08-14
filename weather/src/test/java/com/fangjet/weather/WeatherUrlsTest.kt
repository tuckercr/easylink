package com.fangjet.weather

import com.fangjet.weather.model.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUrlsTest {

    @Test
    fun `default config builds the historical free-tier url exactly`() {
        // This is the URL shape the app has always requested — a regression here
        // would silently change production traffic.
        assertEquals(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=51.5&longitude=-0.12" +
                "&current=temperature_2m,weather_code" +
                "&temperature_unit=celsius",
            WeatherUrls.current(WeatherApiConfig(), 51.5, -0.12, WeatherUnits.METRIC),
        )
    }

    @Test
    fun `a configured api key is appended to both endpoints`() {
        val paid = WeatherApiConfig(
            baseUrl = "https://customer-api.open-meteo.com",
            apiKey = "abc123",
        )
        val current = WeatherUrls.current(paid, 1.0, 2.0, WeatherUnits.IMPERIAL)
        val forecast = WeatherUrls.forecast(paid, 1.0, 2.0, WeatherUnits.IMPERIAL)

        assertTrue(current.startsWith("https://customer-api.open-meteo.com/v1/forecast?"))
        assertTrue(current.endsWith("&apikey=abc123"))
        assertTrue(forecast.startsWith("https://customer-api.open-meteo.com/v1/forecast?"))
        assertTrue(forecast.endsWith("&apikey=abc123"))
    }

    @Test
    fun `a blank api key adds no apikey parameter`() {
        assertFalse(
            WeatherUrls
                .forecast(WeatherApiConfig(), 1.0, 2.0, WeatherUnits.METRIC)
                .contains("apikey"),
        )
    }

    @Test
    fun `a trailing slash on the base url does not double up`() {
        val config = WeatherApiConfig(baseUrl = "https://customer-api.open-meteo.com/")
        assertTrue(
            WeatherUrls
                .current(config, 1.0, 2.0, WeatherUnits.METRIC)
                .startsWith("https://customer-api.open-meteo.com/v1/forecast?"),
        )
    }

    @Test
    fun `forecast url keeps its seven-day daily shape`() {
        val url = WeatherUrls.forecast(WeatherApiConfig(), 1.0, 2.0, WeatherUnits.METRIC)
        assertTrue(url.contains("&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max"))
        assertTrue(url.contains("&forecast_days=7"))
        assertTrue(url.contains("&timezone=auto"))
    }
}
