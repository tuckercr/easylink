package com.fangjet.launcher.domain.usecase

import com.fangjet.launcher.domain.model.AppInfo
import com.fangjet.launcher.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case: fetch the list of installed, launchable apps.
 *
 * Use cases are the heart of Clean Architecture — they encapsulate a single
 * piece of business logic and act as the boundary between the domain and
 * presentation layers. The ViewModel calls the use case; the use case
 * calls the repository. Neither knows about the other's implementation.
 *
 * For a launcher, "business logic" here means: only show apps that have
 * a launcher intent (no system internals), sorted alphabetically.
 * That rule lives here, not scattered across the Fragment or ViewModel.
 */
class GetInstalledAppsUseCase @Inject constructor(
    private val repository: AppRepository,
) {
    /**
     * Returns a [Flow] that emits a fresh, sorted app list whenever
     * the set of installed packages changes.
     */
    operator fun invoke(): Flow<List<AppInfo>> = repository.getInstalledApps()
}
