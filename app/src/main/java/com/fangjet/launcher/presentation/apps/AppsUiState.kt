package com.fangjet.launcher.presentation.apps

import com.fangjet.launcher.domain.model.AppInfo

/** UI state for the All Apps screen. */
sealed class AppsUiState {
    data object Loading : AppsUiState()

    data class Success(
        val apps: List<AppInfo>,
    ) : AppsUiState()

    data class Error(
        val message: String,
    ) : AppsUiState()
}
