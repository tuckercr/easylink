package com.tuckercr.ezlauncher.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.data.preferences.HomePreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val homePrefs: HomePreferencesDataSource,
) : ViewModel() {

    val highContrastEnabled: StateFlow<Boolean> = homePrefs.highContrastEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    fun setHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { homePrefs.setHighContrastEnabled(enabled) }
    }
}
