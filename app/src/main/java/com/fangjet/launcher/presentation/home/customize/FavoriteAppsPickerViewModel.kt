package com.fangjet.launcher.presentation.home.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.data.apps.FavoriteAppsPreferences
import com.fangjet.launcher.data.config.SettingsDefaultsProvider
import com.fangjet.launcher.domain.model.AppInfo
import com.fangjet.launcher.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PickerItem(
    val app: AppInfo,
    val isSelected: Boolean,
)

/**
 * Checkbox list of every installed app for choosing the My Apps row manually.
 * Toggling persists immediately; selection order is preserved (first picked =
 * first in the row).
 */
@HiltViewModel
class FavoriteAppsPickerViewModel @Inject constructor(
    appRepository: AppRepository,
    private val favoritePrefs: FavoriteAppsPreferences,
    settingsDefaults: SettingsDefaultsProvider,
) : ViewModel() {

    /** Selection ceiling — Remote Config-tunable, snapshotted for this screen. */
    val maxApps: Int = settingsDefaults.current().favoriteAppsMaxCount

    val items: StateFlow<List<PickerItem>> = combine(
        appRepository.getInstalledApps(),
        favoritePrefs.customPackages,
    ) { apps, selected ->
        apps.map { PickerItem(app = it, isSelected = it.packageName in selected) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val selectedCount: StateFlow<Int> = combine(
        favoritePrefs.customPackages,
        items,
    ) { selected, _ -> selected.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onToggle(packageName: String) {
        viewModelScope.launch {
            val current = favoritePrefs.customPackages.first()
            val updated = if (packageName in current) {
                current - packageName
            } else {
                if (current.size >= maxApps) return@launch
                current + packageName
            }
            favoritePrefs.setCustomPackages(updated)
        }
    }
}
