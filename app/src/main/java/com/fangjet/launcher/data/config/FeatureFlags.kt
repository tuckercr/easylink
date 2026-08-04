package com.fangjet.launcher.data.config

/**
 * Compile-time feature availability, fixed per product flavor
 * (see app/build.gradle.kts → productFlavors).
 *
 * Injected rather than read from BuildConfig at each call site so classes
 * that behave differently per flavor stay unit-testable in both modes.
 */
data class FeatureFlags(
    /**
     * SOS and fall detection — the features whose permissions (SMS, fine
     * location, health foreground service) are Play-restricted and only the
     * `safety` flavor may request. When false, every entry point to these
     * features must be hidden or a no-op, regardless of stored preferences or
     * Remote Config, because the permissions are not even in the manifest.
     */
    val safetyFeatures: Boolean,
)
