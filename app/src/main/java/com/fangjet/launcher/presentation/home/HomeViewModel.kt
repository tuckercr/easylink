package com.fangjet.launcher.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.data.apps.AppUsageTracker
import com.fangjet.launcher.data.apps.FavoriteAppsPreferences
import com.fangjet.launcher.data.config.SettingsDefaultsProvider
import com.fangjet.launcher.data.notifications.NotificationBadgeRepository
import com.fangjet.launcher.data.preferences.HomePreferencesDataSource
import com.fangjet.launcher.domain.FavoriteAppsSelector
import com.fangjet.launcher.domain.model.AppInfo
import com.fangjet.launcher.domain.model.HomeButton
import com.fangjet.launcher.domain.repository.AppRepository
import com.fangjet.launcher.domain.usecase.LaunchAppUseCase
import com.fangjet.weather.WeatherService
import com.fangjet.weather.model.WeatherInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val launchAppUseCase: LaunchAppUseCase,
    private val homePrefs: HomePreferencesDataSource,
    private val weatherService: WeatherService,
    private val settingsDefaults: SettingsDefaultsProvider,
    appRepository: AppRepository,
    usageTracker: AppUsageTracker,
    favoritePrefs: FavoriteAppsPreferences,
    badgeRepository: NotificationBadgeRepository,
) : ViewModel() {

    // ── Private mutable state ─────────────────────────────────────────────────

    private val isFlashlightOn = MutableStateFlow(false)
    private val weather = MutableStateFlow<WeatherInfo>(WeatherInfo.Loading)

    // ── Public UI state (read-only) ───────────────────────────────────────────

    val uiState: StateFlow<HomeUiState> = combine(
        isFlashlightOn,
        homePrefs.enabledButtons,
        homePrefs.voiceButtonEnabled,
        homePrefs.sosButtonEnabled,
        weather,
    ) { flashlight, buttons, voiceEnabled, sosEnabled, weather ->
        HomeUiState.Success(
            isFlashlightOn = flashlight,
            // Preserve the canonical enum order so the grid is stable
            enabledButtons = HomeButton.entries.filter { it in buttons },
            weather = weather,
            voiceButtonEnabled = voiceEnabled,
            sosButtonEnabled = sosEnabled,
            // Snapshot of the (Remote Config-tunable) default; read here rather than
            // as a 6th combine flow since it only needs to be current at render time.
            sosHoldDurationMs = settingsDefaults.current().sosHoldDurationMs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    // ── Apps Row ──────────────────────────────────────────────────────────────

    /**
     * Apps for the scrollable row; empty when the row is disabled in Settings
     * or the Remote Config kill switch has withdrawn the feature.
     */
    val favoriteApps: StateFlow<List<AppInfo>> = combine(
        favoritePrefs.rowEnabled,
        appRepository.getInstalledApps(),
        usageTracker.launchCounts,
        favoritePrefs.mode,
        favoritePrefs.customPackages,
    ) { enabled, installed, counts, mode, custom ->
        // Remote Config-tunable values; snapshot per emission is current enough.
        val defaults = settingsDefaults.current()
        if (!enabled || !defaults.favoriteAppsFeatureEnabled) {
            emptyList()
        } else {
            FavoriteAppsSelector.select(
                installed = installed,
                launchCounts = counts,
                mode = mode,
                customPackages = custom,
                max = defaults.favoriteAppsMaxCount,
                curatedPool = defaults.favoriteAppsCuratedPool,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** Packages that should show a notification dot; empty unless enabled + granted. */
    val badgedPackages: StateFlow<Set<String>> = combine(
        favoritePrefs.badgesEnabled,
        badgeRepository.badgedPackages,
    ) { enabled, badged ->
        if (enabled) badged else emptySet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptySet(),
    )

    // ── Weather ───────────────────────────────────────────────────────────────

    init {
        fetchWeather()
    }

    /** Re-fetch weather (called on init and when the user grants location). */
    fun refreshWeather() {
        fetchWeather()
    }

    private fun fetchWeather() {
        viewModelScope.launch {
            weather.value = WeatherInfo.Loading
            weather.value = weatherService.fetch()
        }
    }

    // ── User intents ──────────────────────────────────────────────────────────

    fun toggleFlashlight() {
        isFlashlightOn.update { !it }
    }

    /** Set the flashlight to a specific on/off state (used by voice commands). */
    fun setFlashlightEnabled(enabled: Boolean) {
        isFlashlightOn.value = enabled
    }

    fun onAppTapped(packageName: String) {
        viewModelScope.launch { launchAppUseCase(packageName) }
    }

    /** Toggle a home button on/off; persisted to DataStore. */

    /**
     * Hides the "Say a Command" bar — offered when the microphone permission
     * is permanently denied. Re-enableable any time from Settings.
     */
    fun hideVoiceButton() {
        viewModelScope.launch { homePrefs.setVoiceButtonEnabled(false) }
    }

    /** See [HomePreferencesDataSource.micPermissionRequested] for why this exists. */
    val micPermissionRequested: StateFlow<Boolean> = homePrefs.micPermissionRequested
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun markMicPermissionRequested() {
        viewModelScope.launch { homePrefs.markMicPermissionRequested() }
    }

    fun setButtonEnabled(
        button: HomeButton,
        enabled: Boolean,
    ) {
        viewModelScope.launch { homePrefs.setButtonEnabled(button, enabled) }
    }
}
