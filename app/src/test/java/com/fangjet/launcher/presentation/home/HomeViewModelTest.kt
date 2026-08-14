package com.fangjet.launcher.presentation.home

import app.cash.turbine.test
import com.fangjet.launcher.data.apps.AppUsageTracker
import com.fangjet.launcher.data.apps.FavoriteAppsMode
import com.fangjet.launcher.data.apps.FavoriteAppsPreferences
import com.fangjet.launcher.data.config.FakeSettingsDefaultsProvider
import com.fangjet.launcher.data.notifications.NotificationBadgeRepository
import com.fangjet.launcher.data.preferences.HomePreferencesDataSource
import com.fangjet.launcher.data.preferences.PermissionAskPreferences
import com.fangjet.launcher.domain.model.HomeButton
import com.fangjet.launcher.domain.repository.AppRepository
import com.fangjet.launcher.domain.usecase.LaunchAppUseCase
import com.fangjet.shared.config.SettingsDefaults
import com.fangjet.weather.WeatherService
import com.fangjet.weather.model.WeatherInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var launchAppUseCase: LaunchAppUseCase
    private lateinit var homePrefs: HomePreferencesDataSource
    private lateinit var permissionAsks: PermissionAskPreferences
    private lateinit var weatherService: WeatherService
    private var settingsDefaults = FakeSettingsDefaultsProvider()
    private lateinit var appRepository: AppRepository
    private lateinit var usageTracker: AppUsageTracker
    private lateinit var favoritePrefs: FavoriteAppsPreferences
    private lateinit var badgeRepository: NotificationBadgeRepository
    private lateinit var viewModel: HomeViewModel

    private val enabledButtonsFlow = MutableStateFlow(setOf(HomeButton.PHONE))
    private val voiceEnabledFlow = MutableStateFlow(false)
    private val sosEnabledFlow = MutableStateFlow(true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        launchAppUseCase = mockk(relaxed = true)
        homePrefs = mockk(relaxed = true)
        weatherService = mockk(relaxed = true)

        every { homePrefs.enabledButtons } returns enabledButtonsFlow
        every { homePrefs.voiceButtonEnabled } returns voiceEnabledFlow
        every { homePrefs.sosButtonEnabled } returns sosEnabledFlow
        coEvery { weatherService.fetch() } returns WeatherInfo.Unavailable("Network error")

        permissionAsks = mockk(relaxed = true)
        every { permissionAsks.asked(any()) } returns flowOf(false)

        appRepository = mockk(relaxed = true)
        usageTracker = mockk(relaxed = true)
        favoritePrefs = mockk(relaxed = true)
        badgeRepository = mockk(relaxed = true)
        every { appRepository.getInstalledApps() } returns flowOf(emptyList())
        every { usageTracker.launchCounts } returns flowOf(emptyMap())
        every { favoritePrefs.rowEnabled } returns flowOf(true)
        every { favoritePrefs.mode } returns flowOf(FavoriteAppsMode.AUTOMATIC)
        every { favoritePrefs.customPackages } returns flowOf(emptyList())
        every { favoritePrefs.badgesEnabled } returns flowOf(false)
        every { badgeRepository.badgedPackages } returns MutableStateFlow(emptySet())

        viewModel = HomeViewModel(
            launchAppUseCase,
            homePrefs,
            permissionAsks,
            weatherService,
            settingsDefaults,
            appRepository,
            usageTracker,
            favoritePrefs,
            badgeRepository,
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects preferences and flashlight correctly`() =
        runTest {
            viewModel.uiState.test {
                // Skip the initial Loading if it's there
                var item = awaitItem()
                while (item !is HomeUiState.Success) {
                    item = awaitItem()
                }

                assertFalse("Flashlight should be off initially", item.isFlashlightOn)
                assertEquals(listOf(HomeButton.PHONE), item.enabledButtons)
                assertFalse(item.voiceButtonEnabled)

                // Toggle flashlight
                viewModel.toggleFlashlight()
                val afterFlashlight = awaitItem() as HomeUiState.Success
                assertTrue(afterFlashlight.isFlashlightOn)

                // Update preferences
                enabledButtonsFlow.value = setOf(HomeButton.PHONE, HomeButton.CAMERA)
                voiceEnabledFlow.value = true

                // Consume emissions until both updates are present
                var finalSuccess: HomeUiState.Success? = null
                while (finalSuccess == null || finalSuccess.enabledButtons.size < 2 || !finalSuccess.voiceButtonEnabled) {
                    finalSuccess = awaitItem() as HomeUiState.Success
                }

                assertEquals(listOf(HomeButton.PHONE, HomeButton.CAMERA), finalSuccess.enabledButtons)
                assertTrue(finalSuccess.voiceButtonEnabled)
            }
        }

    @Test
    fun `uiState carries the tunable SOS hold duration from defaults`() =
        runTest {
            settingsDefaults = FakeSettingsDefaultsProvider(
                SettingsDefaults.HARDCODED.copy(sosHoldDurationMs = 5_000L),
            )
            viewModel = HomeViewModel(
                launchAppUseCase,
                homePrefs,
                permissionAsks,
                weatherService,
                settingsDefaults,
                appRepository,
                usageTracker,
                favoritePrefs,
                badgeRepository,
            )

            viewModel.uiState.test {
                var item = awaitItem()
                while (item !is HomeUiState.Success) {
                    item = awaitItem()
                }
                assertEquals(5_000L, item.sosHoldDurationMs)
            }
        }

    @Test
    fun `refreshWeather calls weather service`() =
        runTest {
            viewModel.refreshWeather()
            coVerify { weatherService.fetch() }
        }

    @Test
    fun `onAppTapped calls launchAppUseCase`() =
        runTest {
            viewModel.onAppTapped("com.test.app")
            coVerify { launchAppUseCase("com.test.app") }
        }

    @Test
    fun `setButtonEnabled calls data source`() =
        runTest {
            viewModel.setButtonEnabled(HomeButton.CAMERA, true)
            coVerify { homePrefs.setButtonEnabled(HomeButton.CAMERA, true) }
        }

    @Test
    fun `sosButtonEnabled is reflected in uiState`() =
        runTest {
            viewModel.uiState.test {
                var item = awaitItem()
                while (item !is HomeUiState.Success) item = awaitItem()
                val initial = item
                assertTrue("SOS should be on by default", initial.sosButtonEnabled)

                sosEnabledFlow.value = false
                val updated = awaitItem() as HomeUiState.Success
                assertFalse("SOS should reflect preference change", updated.sosButtonEnabled)
            }
        }
}
