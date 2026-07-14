package com.fangjet.launcher.presentation.magnifier

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class MagnifierUiState(
    val zoomLevel: Float = 1f,
    val isHighContrast: Boolean = false,
    val isFlashlightOn: Boolean = false,
) {
    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 8f
        const val ZOOM_STEP = 0.5f
    }
}

/**
 * ViewModel for the Magnifier screen.
 *
 * The actual camera preview is bound in the Fragment (CameraX requires a
 * LifecycleOwner). This ViewModel only manages the control state — zoom
 * level, high contrast mode, flashlight. The Fragment reads these values
 * and applies them to the CameraX [Camera] control instance.
 *
 * This clean separation means the magnifier logic is fully unit-testable
 * without any camera hardware.
 */
@HiltViewModel
class MagnifierViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MagnifierUiState())
    val uiState: StateFlow<MagnifierUiState> = _uiState.asStateFlow()

    fun zoomIn() {
        _uiState.update { state ->
            state.copy(
                zoomLevel = (state.zoomLevel + MagnifierUiState.ZOOM_STEP)
                    .coerceAtMost(MagnifierUiState.MAX_ZOOM),
            )
        }
    }

    fun zoomOut() {
        _uiState.update { state ->
            state.copy(
                zoomLevel = (state.zoomLevel - MagnifierUiState.ZOOM_STEP)
                    .coerceAtLeast(MagnifierUiState.MIN_ZOOM),
            )
        }
    }

    fun toggleHighContrast() {
        _uiState.update { it.copy(isHighContrast = !it.isHighContrast) }
    }

    fun toggleFlashlight() {
        _uiState.update { it.copy(isFlashlightOn = !it.isFlashlightOn) }
    }

    /** Reset all magnifier settings to defaults. */
    fun resetToDefaults() {
        _uiState.value = MagnifierUiState()
    }
}
