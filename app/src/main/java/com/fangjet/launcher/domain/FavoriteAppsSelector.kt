package com.fangjet.launcher.domain

import com.fangjet.launcher.data.apps.FavoriteAppsMode
import com.fangjet.launcher.domain.model.AppInfo

/**
 * Pure selection logic for the Apps Row — which apps show, in which order.
 *
 * Kept free of Android/coroutine machinery so the ranking rules are trivially
 * unit-testable.
 */
object FavoriteAppsSelector {

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
     * @param curatedPool  cold-start fill order (Remote Config-tunable; see
     *   `SettingsDefaults.favoriteAppsCuratedPool`) — may be longer than [max],
     *   uninstalled packages are simply skipped
     *
     * AUTOMATIC: apps ranked by launch count (desc, ties alphabetical); the
     * remainder fills by [curatedPool] then alphabetically, so a fresh install
     * shows a sensible row rather than the first N of the alphabet.
     * CUSTOM: exactly the picked apps, in picked order, skipping uninstalled ones.
     */
    fun select(
        installed: List<AppInfo>,
        launchCounts: Map<String, Long>,
        mode: FavoriteAppsMode,
        customPackages: List<String>,
        max: Int,
        curatedPool: List<String>,
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
                val curatedRank = curatedPool
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
