package com.tuckercr.ezlauncher.presentation.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.domain.usecase.GetInstalledAppsUseCase
import com.tuckercr.ezlauncher.domain.usecase.LaunchAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        .map<_, AppsUiState> { AppsUiState.Success(it) }
        .catch { emit(AppsUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppsUiState.Loading,
        )

    fun onAppTapped(packageName: String) {
        viewModelScope.launch { launchApp(packageName) }
    }
}
