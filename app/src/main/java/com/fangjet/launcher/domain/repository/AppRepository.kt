package com.fangjet.launcher.domain.repository

import com.fangjet.launcher.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for all data the launcher needs.
 *
 * This interface lives in the domain layer; implementations live in the
 * data layer. The domain never imports the data layer — only the DI
 * module wires them together. This is the Dependency Inversion Principle
 * in action and is what makes the ViewModels fully unit-testable with mocks.
 */
interface AppRepository {

    /**
     * Emits the sorted list of user-launchable installed apps.
     * Re-emits whenever a package is installed or removed.
     */
    fun getInstalledApps(): Flow<List<AppInfo>>

    /**
     * Launches the app identified by [packageName].
     * Returns true if a launch intent was found and started.
     */
    suspend fun launchApp(packageName: String): Boolean
}
