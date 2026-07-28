package com.fangjet.launcher.data.config

import android.content.Context
import android.util.Log
import com.fangjet.launcher.BuildConfig
import com.fangjet.shared.config.RemoteConfigKeys
import com.fangjet.shared.config.RemoteValueReader
import com.fangjet.shared.config.SettingsDefaults
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [SettingsDefaultsProvider] backed by Firebase Remote Config.
 *
 * ## Self-guarding
 *
 * Firebase is not yet wired in this project (no `google-services.json`, the
 * `google-services` plugin is off), so [FirebaseApp] never initialises. This
 * class checks for that on construction: when Firebase is absent it becomes an
 * inert pass-through that always returns [SettingsDefaults.HARDCODED], and
 * [refresh] does nothing. Nothing here throws in that state, so it is safe to
 * bind and ship today.
 *
 * Once Firebase is added (see README → "Remote Config"), the same code lights up
 * automatically: it seeds in-app defaults from [SettingsDefaults.HARDCODED],
 * reads the last activated values immediately on cold start, and [refresh] pulls
 * newer values from the server.
 */
@Singleton
class RemoteConfigSettingsDefaultsProvider
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : SettingsDefaultsProvider {

        /** Non-null only when Firebase actually initialised. */
        private val remoteConfig: FirebaseRemoteConfig? = initRemoteConfig(context)

        @Volatile
        private var cached: SettingsDefaults = SettingsDefaults.HARDCODED

        init {
            // Cold-start values: Remote Config persists the last activated fetch to
            // disk, so we can surface server values before the first network call.
            remoteConfig?.let { cached = readFrom(it) }
        }

        override fun current(): SettingsDefaults = cached

        override suspend fun refresh() {
            val rc = remoteConfig ?: return
            try {
                rc.fetchAndActivate().awaitCompat()
                cached = readFrom(rc)
            } catch (e: Exception) {
                // Network failure, throttling, etc. — keep whatever we already had.
                Log.w(TAG, "Remote Config refresh failed; keeping current defaults", e)
            }
        }

        private fun initRemoteConfig(context: Context): FirebaseRemoteConfig? {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.i(TAG, "Firebase not initialised — using hardcoded setting defaults")
                return null
            }
            return try {
                FirebaseRemoteConfig.getInstance().apply {
                    setConfigSettingsAsync(
                        FirebaseRemoteConfigSettings
                            .Builder()
                            // Debug: always fetch fresh so console changes show up immediately.
                            // Release: 1h honours Google's throttling and saves battery/data.
                            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3_600)
                            .build(),
                    )
                    setDefaultsAsync(IN_APP_DEFAULTS)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Remote Config unavailable; using hardcoded defaults", e)
                null
            }
        }

        private fun readFrom(rc: FirebaseRemoteConfig): SettingsDefaults =
            SettingsDefaults.from(
                object : RemoteValueReader {
                    override fun boolean(key: String) = rc.getBoolean(key)

                    override fun long(key: String) = rc.getLong(key)

                    override fun string(key: String) = rc.getString(key).ifEmpty { null }
                },
            )

        companion object {
            private const val TAG = "RemoteConfigDefaults"

            /**
             * In-app defaults seeded into Remote Config so lookups return sensible
             * values before any fetch. Mirrors [SettingsDefaults.HARDCODED] exactly.
             */
            private val IN_APP_DEFAULTS: Map<String, Any> = mapOf(
                RemoteConfigKeys.SOS_BUTTON_VISIBLE_BY_DEFAULT to SettingsDefaults.HARDCODED.sosButtonVisible,
                RemoteConfigKeys.VOICE_BUTTON_VISIBLE_BY_DEFAULT to SettingsDefaults.HARDCODED.voiceButtonVisible,
                RemoteConfigKeys.HIGH_CONTRAST_BY_DEFAULT to SettingsDefaults.HARDCODED.highContrast,
                RemoteConfigKeys.FALL_DETECTION_ENABLED_BY_DEFAULT to SettingsDefaults.HARDCODED.fallDetectionEnabled,
                RemoteConfigKeys.FALL_SENSITIVITY_DEFAULT to SettingsDefaults.HARDCODED.fallSensitivity,
                RemoteConfigKeys.SOS_HOLD_DURATION_MS to SettingsDefaults.HARDCODED.sosHoldDurationMs,
                RemoteConfigKeys.SOS_COUNTDOWN_SECONDS to SettingsDefaults.HARDCODED.sosCountdownSeconds,
                RemoteConfigKeys.FAVORITE_APPS_MAX_COUNT to SettingsDefaults.HARDCODED.favoriteAppsMaxCount,
            )
        }
    }

/** Bridges a Play-services [Task] to a coroutine without the extra coroutines-play-services dep. */
private suspend fun <T> Task<T>.awaitCompat(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
