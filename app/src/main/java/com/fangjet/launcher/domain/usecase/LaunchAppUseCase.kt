package com.fangjet.launcher.domain.usecase

import com.fangjet.launcher.data.apps.AppUsageTracker
import com.fangjet.launcher.domain.repository.AppRepository
import javax.inject.Inject

/**
 * Use Case: launch an installed application by package name.
 *
 * Wrapping this in a use case means the ViewModel stays lean and the
 * launch logic has a single, testable home. It is also the choke point
 * every app launch flows through (Home, All Apps, voice commands), which
 * is what lets [AppUsageTracker] rank "most used" apps with no special
 * permissions.
 */
class LaunchAppUseCase @Inject constructor(
    private val repository: AppRepository,
    private val usageTracker: AppUsageTracker,
) {
    /**
     * @param packageName The package to launch (e.g. "com.google.android.dialer")
     * @return true if the app was successfully started; false if no launch
     *         intent exists (e.g. a system package with no launcher activity).
     */
    suspend operator fun invoke(packageName: String): Boolean {
        val launched = repository.launchApp(packageName)
        if (launched) usageTracker.recordLaunch(packageName)
        return launched
    }
}
