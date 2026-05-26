package com.tuckercr.ezlauncher.presentation.home.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.data.preferences.HomePreferencesDataSource
import com.tuckercr.ezlauncher.domain.model.HomeButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ButtonToggleItem(
    val button: HomeButton,
    val label: String,
    val isEnabled: Boolean,
)

@HiltViewModel
class CustomizeHomeViewModel @Inject constructor(
    private val homePrefs: HomePreferencesDataSource,
) : ViewModel() {

    val items: StateFlow<List<ButtonToggleItem>> = homePrefs.enabledButtons
        .map { enabled ->
            HomeButton.entries.map { btn ->
                ButtonToggleItem(
                    button    = btn,
                    label     = btn.defaultLabel,
                    isEnabled = btn in enabled,
                )
            }
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeButton.entries.map { ButtonToggleItem(it, it.defaultLabel, true) },
        )

    fun toggle(button: HomeButton, enabled: Boolean) {
        viewModelScope.launch { homePrefs.setButtonEnabled(button, enabled) }
    }
}
