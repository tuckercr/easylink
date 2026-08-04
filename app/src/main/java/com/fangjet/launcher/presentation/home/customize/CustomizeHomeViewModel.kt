package com.fangjet.launcher.presentation.home.customize

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.data.apps.FavoriteAppsMode
import com.fangjet.launcher.data.apps.FavoriteAppsPreferences
import com.fangjet.launcher.data.config.SettingsDefaultsProvider
import com.fangjet.launcher.data.fall.FallDetectionManager
import com.fangjet.launcher.data.home.DefaultHomeChecker
import com.fangjet.launcher.data.notifications.NotificationBadgeRepository
import com.fangjet.launcher.data.preferences.FallDetectionPreferences
import com.fangjet.launcher.data.preferences.HomePreferencesDataSource
import com.fangjet.launcher.domain.model.FallSensitivity
import com.fangjet.launcher.domain.model.HomeButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ButtonToggleItem(
    val button: HomeButton,
    @param:StringRes val labelRes: Int,
    val isEnabled: Boolean,
)

data class CustomizeUiState(
    val buttons: List<ButtonToggleItem> = emptyList(),
    val fallDetectionEnabled: Boolean = false,
    val fallSensitivity: FallSensitivity = FallSensitivity.MEDIUM,
    val voiceButtonEnabled: Boolean = false,
    val sosButtonEnabled: Boolean = true,
    /** Remote Config kill switch — false hides the voice toggle entirely. */
    val voiceFeatureEnabled: Boolean = true,
)

/** Settings state for the Apps Row section. */
data class FavoriteAppsSettingsState(
    val rowEnabled: Boolean = true,
    val isAutomatic: Boolean = true,
    val badgesEnabled: Boolean = false,
    /** False when the user still needs to grant Notification access. */
    val badgeAccessGranted: Boolean = false,
    /** Remote Config kill switch — false hides the whole section. */
    val featureEnabled: Boolean = true,
)

/** State for the Home App section (default-launcher status). */
data class HomeAppState(
    val isDefault: Boolean = true,
    /** Label of the current default home app (e.g. "Pixel Launcher"), null if unknown. */
    val currentHomeLabel: String? = null,
)

@HiltViewModel
class CustomizeHomeViewModel @Inject constructor(
    private val homePrefs: HomePreferencesDataSource,
    private val fallPrefs: FallDetectionPreferences,
    private val fallManager: FallDetectionManager,
    private val favoritePrefs: FavoriteAppsPreferences,
    private val badgeRepository: NotificationBadgeRepository,
    private val defaultHomeChecker: DefaultHomeChecker,
    private val settingsDefaults: SettingsDefaultsProvider,
) : ViewModel() {

    /** Bumped on screen resume so system-derived state re-reads after Settings. */
    private val systemStatusRefresh = MutableStateFlow(0)

    val favoriteAppsState: StateFlow<FavoriteAppsSettingsState> = combine(
        favoritePrefs.rowEnabled,
        favoritePrefs.mode,
        favoritePrefs.badgesEnabled,
        systemStatusRefresh,
    ) { rowEnabled, mode, badges, _ ->
        FavoriteAppsSettingsState(
            rowEnabled = rowEnabled,
            isAutomatic = mode == FavoriteAppsMode.AUTOMATIC,
            badgesEnabled = badges,
            badgeAccessGranted = badgeRepository.isListenerPermissionGranted(),
            featureEnabled = settingsDefaults.current().favoriteAppsFeatureEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoriteAppsSettingsState(),
    )

    /** Default-launcher status; re-read on every [refreshSystemStatus]. */
    val homeAppState: StateFlow<HomeAppState> = systemStatusRefresh
        .map {
            HomeAppState(
                isDefault = defaultHomeChecker.isDefault(),
                currentHomeLabel = defaultHomeChecker.defaultHomeLabel(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeAppState(),
        )

    /**
     * Re-reads state owned by the OS (notification access, default-home role)
     * that can change while the user is away in system settings.
     */
    fun refreshSystemStatus() {
        systemStatusRefresh.value++
    }

    fun setFavoriteRowEnabled(enabled: Boolean) {
        viewModelScope.launch { favoritePrefs.setRowEnabled(enabled) }
    }

    fun setFavoriteAutomatic(automatic: Boolean) {
        viewModelScope.launch {
            favoritePrefs.setMode(
                if (automatic) FavoriteAppsMode.AUTOMATIC else FavoriteAppsMode.CUSTOM,
            )
        }
    }

    fun setBadgesEnabled(enabled: Boolean) {
        viewModelScope.launch { favoritePrefs.setBadgesEnabled(enabled) }
    }

    val uiState: StateFlow<CustomizeUiState> = combine(
        homePrefs.enabledButtons,
        homePrefs.voiceButtonEnabled,
        homePrefs.sosButtonEnabled,
        fallPrefs.isEnabled,
        fallPrefs.sensitivity,
    ) { enabled, voiceEnabled, sosEnabled, fallEnabled, sensitivity ->
        CustomizeUiState(
            buttons = HomeButton.entries.map { btn ->
                ButtonToggleItem(btn, btn.labelRes, btn in enabled)
            },
            voiceButtonEnabled = voiceEnabled,
            sosButtonEnabled = sosEnabled,
            fallDetectionEnabled = fallEnabled,
            fallSensitivity = sensitivity,
            voiceFeatureEnabled = settingsDefaults.current().voiceCommandsFeatureEnabled,
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
