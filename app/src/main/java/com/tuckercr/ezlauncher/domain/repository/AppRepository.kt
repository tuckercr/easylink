package com.tuckercr.ezlauncher.domain.repository

import com.tuckercr.ezlauncher.domain.model.AppInfo
import com.tuckercr.ezlauncher.domain.model.BatteryState
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
     * Emits the current battery state and re-emits on every change.
     * The first emission arrives quickly (it queries the sticky broadcast).
     */
    fun observeBatteryState(): Flow<BatteryState>

    /**
     * Launches the app identified by [packageName].
     * Returns true if a launch intent was found and started.
     */
    suspend fun launchApp(packageName: String): Boolean
}
