package com.fangjet.launcher.presentation.home.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.data.fall.FallDetectionManager
import com.fangjet.launcher.data.preferences.FallDetectionPreferences
import com.fangjet.launcher.data.preferences.HomePreferencesDataSource
import com.fangjet.launcher.domain.model.FallSensitivity
import com.fangjet.launcher.domain.model.HomeButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ButtonToggleItem(
    val button: HomeButton,
    val label: String,
    val isEnabled: Boolean,
)

data class CustomizeUiState(
    val buttons: List<ButtonToggleItem> = emptyList(),
    val fallDetectionEnabled: Boolean = false,
    val fallSensitivity: FallSensitivity = FallSensitivity.MEDIUM,
    val voiceButtonEnabled: Boolean = false,
    val sosButtonEnabled: Boolean = true,
)

@HiltViewModel
class CustomizeHomeViewModel @Inject constructor(
    private val homePrefs: HomePreferencesDataSource,
    private val fallPrefs: FallDetectionPreferences,
    private val fallManager: FallDetectionManager,
) : ViewModel() {

    val uiState: StateFlow<CustomizeUiState> = combine(
        homePrefs.enabledButtons,
        homePrefs.voiceButtonEnabled,
        homePrefs.sosButtonEnabled,
        fallPrefs.isEnabled,
        fallPrefs.sensitivity,
    ) { enabled, voiceEnabled, sosEnabled, fallEnabled, sensitivity ->
        CustomizeUiState(
            buttons = HomeButton.entries.map { btn ->
                ButtonToggleItem(btn, btn.defaultLabel, btn in enabled)
            },
            voiceButtonEnabled = voiceEnabled,
            sosButtonEnabled = sosEnabled,
            fallDetectionEnabled = fallEnabled,
            fallSensitivity = sensitivity,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CustomizeUiState(),
    )

    // ── Home buttons ──────────────────────────────────────────────────────────

    fun toggle(
        button: HomeButton,
        enabled: Boolean,
    ) {
        viewModelScope.launch { homePrefs.setButtonEnabled(button, enabled) }
    }

    // ── Voice button ──────────────────────────────────────────────────────────

    fun setVoiceButtonEnabled(enabled: Boolean) {
        viewModelScope.launch { homePrefs.setVoiceButtonEnabled(enabled) }
    }

    // ── SOS button ────────────────────────────────────────────────────────────

    fun setSosButtonEnabled(enabled: Boolean) {
        viewModelScope.launch { homePrefs.setSosButtonEnabled(enabled) }
    }

    // ── High-contrast theme ───────────────────────────────────────────────────

    val highContrastEnabled: StateFlow<Boolean> = homePrefs.highContrastEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { homePrefs.setHighContrastEnabled(enabled) }
    }

    // ── Fall detection ────────────────────────────────────────────────────────

    fun setFallDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            fallPrefs.setEnabled(enabled)
            if (enabled) fallManager.start() else fallManager.stop()
        }
    }

    fun setFallSensitivity(sensitivity: FallSensitivity) {
        viewModelScope.launch { fallPrefs.setSensitivity(sensitivity) }
    }
}
