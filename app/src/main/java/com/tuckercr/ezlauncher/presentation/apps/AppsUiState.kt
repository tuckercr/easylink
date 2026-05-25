package com.tuckercr.ezlauncher.presentation.apps

import com.tuckercr.ezlauncher.domain.model.AppInfo

/** UI state for the All Apps screen. */
sealed class AppsUiState {
    data object Loading : AppsUiState()
    data class Success(val apps: List<AppInfo>) : AppsUiState()
    data class Error(val message: String) : AppsUiState()
}
