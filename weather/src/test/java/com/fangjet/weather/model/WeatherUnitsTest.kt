package com.fangjet.weather.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WeatherUnitsTest {

    @Test
    fun `US and territories use imperial`() {
        assertEquals(WeatherUnits.IMPERIAL, WeatherUnits.fromLocale(Locale.US))
        assertEquals(WeatherUnits.IMPERIAL, WeatherUnits.fromLocale(Locale("en", "PR")))
        assertEquals(WeatherUnits.IMPERIAL, WeatherUnits.fromLocale(Locale("en", "GU")))
        assertEquals(WeatherUnits.IMPERIAL, WeatherUnits.fromLocale(Locale("en", "VI")))
        assertEquals(WeatherUnits.IMPERIAL, WeatherUnits.fromLocale(Locale("en", "AS")))
    }

    @Test
    fun `UK and others use metric`() {
        assertEquals(WeatherUnits.METRIC, WeatherUnits.fromLocale(Locale.UK))
        assertEquals(WeatherUnits.METRIC, WeatherUnits.fromLocale(Locale.CANADA))
        assertEquals(WeatherUnits.METRIC, WeatherUnits.fromLocale(Locale.GERMANY))
        assertEquals(WeatherUnits.METRIC, WeatherUnits.fromLocale(Locale("en", "AU")))
    }

    @Test
    fun `apiParam matches Open-Meteo parameter values`() {
        assertEquals("celsius", WeatherUnits.METRIC.apiParam)
        assertEquals("fahrenheit", WeatherUnits.IMPERIAL.apiParam)
    }

    @Test
    fun `symbol returns correct degree suffix`() {
        assertEquals("°C", WeatherUnits.METRIC.symbol)
        assertEquals("°F", WeatherUnits.IMPERIAL.symbol)
    }
}
