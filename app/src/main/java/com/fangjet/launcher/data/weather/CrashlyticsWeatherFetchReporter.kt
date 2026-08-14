package com.fangjet.launcher.data.weather

import android.util.Log
import com.fangjet.launcher.BuildConfig
import com.fangjet.weather.WeatherFetchReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records weather API failures as Crashlytics non-fatals.
 *
 * This is the production early-warning system for Open-Meteo rate limiting:
 * a rise in HTTP 429 non-fatals on the Crashlytics dashboard is the signal to
 * flip the `weather_api_base_url` / `weather_api_key` Remote Config values to
 * the paid endpoint — no app release needed. The custom keys make the
 * dashboard filterable by status code.
 *
 * Debug builds only log — local development against a flaky network must not
 * pollute the production trend.
 */
@Singleton
class CrashlyticsWeatherFetchReporter @Inject constructor() : WeatherFetchReporter {

    override fun onHttpError(
        httpCode: Int,
        endpoint: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "Weather API HTTP $httpCode ($endpoint) — debug build, not reported")
            return
        }
        // Telemetry must never break weather itself.
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey(KEY_HTTP_CODE, httpCode)
                setCustomKey(KEY_ENDPOINT, endpoint)
                recordException(WeatherApiHttpException(httpCode, endpoint))
            }
        }
    }

    companion object {
        private const val TAG = "WeatherFetchReporter"
        private const val KEY_HTTP_CODE = "weather_http_code"
        private const val KEY_ENDPOINT = "weather_endpoint"
    }
}

/** Named exception type so weather API failures group as their own Crashlytics issue. */
class WeatherApiHttpException(
    httpCode: Int,
    endpoint: String,
) : Exception("Weather API returned HTTP $httpCode for the $endpoint endpoint")
