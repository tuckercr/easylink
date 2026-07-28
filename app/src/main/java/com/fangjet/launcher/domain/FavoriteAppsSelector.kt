package com.fangjet.launcher.domain

import com.fangjet.launcher.data.apps.FavoriteAppsMode
import com.fangjet.launcher.domain.model.AppInfo

/**
 * Pure selection logic for the My Apps row — which apps show, in which order.
 *
 * Kept free of Android/coroutine machinery so the ranking rules are trivially
 * unit-testable.
 */
object FavoriteAppsSelector {

    /**
     * Cold-start fill order: apps this audience actually opens, most likely
     * first. Android has no permission-free "recently used" API (UsageStats
     * needs a special-access grant), so before any local launch history exists
     * this list decides the row instead of the alphabet. Uninstalled entries
     * are simply skipped.
     */
    val CURATED_PRIORITY: List<String> = listOf(
        "com.whatsapp",
        "com.google.android.apps.messaging",
        "com.google.android.apps.photos",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.android.chrome",
        "com.google.android.gm",
        "com.google.android.apps.maps",
        "com.google.android.calendar",
        "com.spotify.music",
        "com.audible.application",
        "com.netflix.mediaclient",
        "com.amazon.kindle",
        "com.instagram.android",
        "com.google.android.contacts",
        "com.google.android.deskclock",
    )

    /**
     * Never auto-fill the row with our own apps — the elder doesn't need the
     * caregiver app (or the launcher itself) taking a slot. An explicit CUSTOM
     * pick still wins; this only shapes the automatic ranking.
     */
    private const val EXCLUDED_PREFIX = "com.fangjet."

    /**
     * @param installed  every launchable app on the device
     * @param launchCounts  package → launch count from AppUsageTracker
     * @param mode  AUTOMATIC (by usage) or CUSTOM (user's picks)
     * @param customPackages  ordered picks for CUSTOM mode
     * @param max  row length cap (Remote Config-tunable)
     *
     * AUTOMATIC: apps ranked by launch count (desc, ties alphabetical); the
     * remainder fills by [CURATED_PRIORITY] then alphabetically, so a fresh
     * install shows a sensible row rather than the first N of the alphabet.
     * CUSTOM: exactly the picked apps, in picked order, skipping uninstalled ones.
     */
    fun select(
        installed: List<AppInfo>,
        launchCounts: Map<String, Long>,
        mode: FavoriteAppsMode,
        customPackages: List<String>,
        max: Int,
    ): List<AppInfo> {
        if (max <= 0) return emptyList()
        return when (mode) {
            FavoriteAppsMode.CUSTOM -> {
                val byPackage = installed.associateBy { it.packageName }
                customPackages.mapNotNull { byPackage[it] }.take(max)
            }

            FavoriteAppsMode.AUTOMATIC -> {
                val eligible = installed.filterNot { it.packageName.startsWith(EXCLUDED_PREFIX) }
                val used = eligible
                    .filter { (launchCounts[it.packageName] ?: 0L) > 0L }
                    .sortedWith(
                        compareByDescending<AppInfo> { launchCounts[it.packageName] ?: 0L }
                            .thenBy { it.label.lowercase() },
                    )
                val curatedRank = CURATED_PRIORITY
                    .withIndex()
                    .associate { (index, pkg) -> pkg to index }
                val fill = eligible
                    .filter { (launchCounts[it.packageName] ?: 0L) == 0L }
                    .sortedWith(
                        compareBy<AppInfo> { curatedRank[it.packageName] ?: Int.MAX_VALUE }
                            .thenBy { it.label.lowercase() },
                    )
                (used + fill).take(max)
            }
        }
    }
}
