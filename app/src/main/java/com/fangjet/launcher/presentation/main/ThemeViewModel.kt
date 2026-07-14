package com.fangjet.launcher.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.data.preferences.HomePreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val homePrefs: HomePreferencesDataSource,
) : ViewModel() {

    // Seeded synchronously so the very first Compose frame already reflects the saved
    // preference — Eagerly alone doesn't help, since DataStore's first disk read is async
    // and the flow wouldn't emit in time for MainActivity's first setContent frame.
    val highContrastEnabled: StateFlow<Boolean> = homePrefs.highContrastEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = runBlocking { homePrefs.highContrastEnabled.first() },
        )

    fun setHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { homePrefs.setHighContrastEnabled(enabled) }
    }
}
