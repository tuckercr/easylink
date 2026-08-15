package com.fangjet.launcher.presentation.home.customize

import app.cash.turbine.test
import com.fangjet.launcher.data.apps.FavoriteAppsMode
import com.fangjet.launcher.data.apps.FavoriteAppsPreferences
import com.fangjet.launcher.data.apps.InstalledAppChecker
import com.fangjet.launcher.data.config.FakeSettingsDefaultsProvider
import com.fangjet.launcher.data.fall.FallDetectionManager
import com.fangjet.launcher.data.home.DefaultHomeChecker
import com.fangjet.launcher.data.notifications.NotificationBadgeRepository
import com.fangjet.launcher.data.preferences.FallDetectionPreferences
import com.fangjet.launcher.data.preferences.HomePreferencesDataSource
import com.fangjet.launcher.domain.model.FallSensitivity
import com.fangjet.launcher.domain.model.HomeButton
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
class CustomizeHomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var homePrefs: HomePreferencesDataSource
    private lateinit var installedAppChecker: InstalledAppChecker
    private lateinit var fallPrefs: FallDetectionPreferences
    private lateinit var fallManager: FallDetectionManager
    private lateinit var defaultHomeChecker: DefaultHomeChecker
    private lateinit var viewModel: CustomizeHomeViewModel

    private val enabledButtonsFlow = MutableStateFlow(setOf(HomeButton.PHONE))
    private val voiceEnabledFlow = MutableStateFlow(false)
    private val sosEnabledFlow = MutableStateFlow(true)
    private val highContrastFlow = MutableStateFlow(false)
    private val fallEnabledFlow = MutableStateFlow(false)
    private val fallSensitivityFlow = MutableStateFlow(FallSensitivity.MEDIUM)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        homePrefs = mockk(relaxed = true)
        fallPrefs = mockk(relaxed = true)
        fallManager = mockk(relaxed = true)

        every { homePrefs.enabledButtons } returns enabledButtonsFlow
        every { homePrefs.voiceButtonEnabled } returns voiceEnabledFlow
        every { homePrefs.sosButtonEnabled } returns sosEnabledFlow
        every { homePrefs.highContrastEnabled } returns highContrastFlow
        every { fallPrefs.isEnabled } returns fallEnabledFlow
        every { fallPrefs.sensitivity } returns fallSensitivityFlow

        val favoritePrefs = mockk<FavoriteAppsPreferences>(relaxed = true)
        every { favoritePrefs.rowEnabled } returns flowOf(true)
        every { favoritePrefs.mode } returns flowOf(FavoriteAppsMode.AUTOMATIC)
        every { favoritePrefs.badgesEnabled } returns flowOf(false)
        val badgeRepository = mockk<NotificationBadgeRepository>(relaxed = true)
        defaultHomeChecker = mockk(relaxed = true)
        every { defaultHomeChecker.isDefault() } returns true
        every { defaultHomeChecker.defaultHomeLabel() } returns "EasyLink"
        installedAppChecker = mockk(relaxed = true)
        every { installedAppChecker.isInstalled(any()) } returns false
        viewModel = CustomizeHomeViewModel(
            homePrefs,
            installedAppChecker,
            fallPrefs,
            fallManager,
            favoritePrefs,
            badgeRepository,
            defaultHomeChecker,
            FakeSettingsDefaultsProvider(),
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects preferences correctly`() =
        runTest {
            viewModel.uiState.test {
                // Skip initial default state if empty
                var item = awaitItem()
                while (item.buttons.isEmpty()) {
                    item = awaitItem()
                }

                assertTrue(
                    "PHONE should be enabled",
                    item.buttons.any { it.button == HomeButton.PHONE && it.isEnabled },
                )
                assertFalse(item.voiceButtonEnabled)
                assertFalse(item.fallDetectionEnabled)
                assertEquals(FallSensitivity.MEDIUM, item.fallSensitivity)

                // Update preferences
                enabledButtonsFlow.value = setOf(HomeButton.PHONE, HomeButton.CAMERA)
                voiceEnabledFlow.value = true
                fallEnabledFlow.value = true
                fallSensitivityFlow.value = FallSensitivity.HIGH

                // Consume emissions until all updates are reflected
                var finalState: CustomizeUiState? = null
                while (finalState == null ||
                    finalState.buttons.count { it.isEnabled } < 2 ||
                    !finalState.voiceButtonEnabled ||
                    !finalState.fallDetectionEnabled ||
                    finalState.fallSensitivity != FallSensitivity.HIGH
                ) {
                    finalState = awaitItem()
                }

                assertTrue(finalState.buttons.any { it.button == HomeButton.CAMERA && it.isEnabled })
                assertTrue(finalState.voiceButtonEnabled)
                assertTrue(finalState.fallDetectionEnabled)
                assertEquals(FallSensitivity.HIGH, finalState.fallSensitivity)
            }
        }

    @Test
    fun `toggle button calls data source`() =
        runTest {
            viewModel.toggle(HomeButton.CAMERA, true)
            coVerify { homePrefs.setButtonEnabled(HomeButton.CAMERA, true) }
        }

    @Test
    fun `setVoiceButtonEnabled calls data source`() =
        runTest {
            viewModel.setVoiceButtonEnabled(true)
            coVerify { homePrefs.setVoiceButtonEnabled(true) }
        }

    @Test
    fun `setFallDetectionEnabled true calls prefs and starts manager`() =
        runTest {
            viewModel.setFallDetectionEnabled(true)
            coVerify { fallPrefs.setEnabled(true) }
            coVerify { fallManager.start() }
        }

    @Test
    fun `setFallDetectionEnabled false calls prefs and stops manager`() =
        runTest {
            viewModel.setFallDetectionEnabled(false)
            coVerify { fallPrefs.setEnabled(false) }
            coVerify { fallManager.stop() }
        }

    @Test
    fun `setFallSensitivity calls data source`() =
        runTest {
            viewModel.setFallSensitivity(FallSensitivity.LOW)
            coVerify { fallPrefs.setSensitivity(FallSensitivity.LOW) }
        }

    @Test
    fun `highContrastEnabled reflects homePrefs flow`() =
        runTest {
            viewModel.highContrastEnabled.test {
                assertFalse(awaitItem())
                highContrastFlow.value = true
                assertTrue(awaitItem())
                highContrastFlow.value = false
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `setHighContrastEnabled calls data source`() =
        runTest {
            viewModel.setHighContrastEnabled(true)
            coVerify { homePrefs.setHighContrastEnabled(true) }
        }

    @Test
    fun `setSosButtonEnabled calls data source`() =
        runTest {
            viewModel.setSosButtonEnabled(false)
            coVerify { homePrefs.setSosButtonEnabled(false) }
        }

    // ── Home App section (default-launcher status) ────────────────────────────

    @Test
    fun `homeAppState reflects the default-home checker`() =
        runTest {
            viewModel.homeAppState.test {
                val state = awaitItem()
                assertTrue(state.isDefault)
                assertEquals("EasyLink", state.currentHomeLabel)
            }
        }

    @Test
    fun `refreshSystemStatus re-reads the default-home role`() =
        runTest {
            viewModel.homeAppState.test {
                assertTrue(awaitItem().isDefault)

                // The user switches launchers in system settings, then comes back.
                every { defaultHomeChecker.isDefault() } returns false
                every { defaultHomeChecker.defaultHomeLabel() } returns "Pixel Launcher"
                viewModel.refreshSystemStatus()

                val state = awaitItem()
                assertFalse(state.isDefault)
                assertEquals("Pixel Launcher", state.currentHomeLabel)
            }
        }
}
