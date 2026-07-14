package com.fangjet.launcher.domain.usecase

import com.fangjet.launcher.domain.repository.AppRepository
import javax.inject.Inject

/**
 * Use Case: launch an installed application by package name.
 *
 * Wrapping this in a use case means the ViewModel stays lean and the
 * launch logic (and any future auditing / analytics hooks) has a
 * single, testable home.
 */
class LaunchAppUseCase @Inject constructor(
    private val repository: AppRepository,
) {
    /**
     * @param packageName The package to launch (e.g. "com.google.android.dialer")
     * @return true if the app was successfully started; false if no launch
     *         intent exists (e.g. a system package with no launcher activity).
     */
    suspend operator fun invoke(packageName: String): Boolean = repository.launchApp(packageName)
}
