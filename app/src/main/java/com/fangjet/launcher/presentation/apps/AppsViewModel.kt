package com.fangjet.launcher.presentation.apps

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.data.cache.AppListCache
import com.fangjet.launcher.domain.usecase.GetInstalledAppsUseCase
import com.fangjet.launcher.domain.usecase.LaunchAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AppsViewModel"

/**
 * ViewModel for the All Apps screen.
 *
 * Demonstrates the use of a UseCase (rather than the repository directly)
 * at the ViewModel boundary — this is the Clean Architecture call chain:
 *
 *   AppsFragment → AppsViewModel → GetInstalledAppsUseCase → AppRepository → Android PM
 */
@HiltViewModel
class AppsViewModel @Inject constructor(
    getInstalledApps: GetInstalledAppsUseCase,
    private val launchApp: LaunchAppUseCase,
) : ViewModel() {

    val uiState: StateFlow<AppsUiState> = getInstalledApps()
        .onEach { apps ->
            if (apps.isEmpty()) {
                Log.w(TAG, "getInstalledApps emitted an empty list — UI will show 'No Apps Found'")
            } else {
                Log.d(TAG, "getInstalledApps emitted ${apps.size} apps")
            }
        }.map<_, AppsUiState> { AppsUiState.Success(it) }
        .catch {
            Log.e(TAG, "getInstalledApps error: ${it.message}", it)
            emit(AppsUiState.Error(it.message ?: "Unknown error"))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // Seed from pre-warmed cache for instant visibility, falling back to Loading if empty.
            initialValue = AppListCache.apps.value
                .takeIf { it.isNotEmpty() }
                ?.let { AppsUiState.Success(it) }
                ?: AppsUiState.Loading,
        )

    fun onAppTapped(packageName: String) {
        viewModelScope.launch { launchApp(packageName) }
    }
}
