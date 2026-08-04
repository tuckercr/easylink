package com.fangjet.launcher.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.fangjet.launcher.data.config.FakeSettingsDefaultsProvider
import com.fangjet.launcher.data.config.FeatureFlags
import com.fangjet.launcher.domain.model.HomeButton
import com.fangjet.shared.config.SettingsDefaults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests for [HomePreferencesDataSource].
 *
 * Verifies the two-key storage strategy introduced when optional home-screen
 * buttons (defaultEnabled = false) were added:
 *   - DISABLED_BUTTONS  — tracks which default-on buttons the user turned off
 *   - ENABLED_OPTIONAL_BUTTONS — tracks which opt-in buttons the user turned on
 *
 * A fresh DataStore (backed by a temporary file) is created per test so there
 * is no shared state between tests. [UnconfinedTestDispatcher] is used so
 * DataStore writes complete eagerly without needing [advanceUntilIdle].
 */
private val SAFETY = FeatureFlags(safetyFeatures = true)
private val STANDARD = FeatureFlags(safetyFeatures = false)

@OptIn(ExperimentalCoroutinesApi::class)
class HomePreferencesDataSourceTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataSource: HomePreferencesDataSource

    @Before
    fun setup() {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("home_prefs_test.preferences_pb") },
        )
        dataSource = HomePreferencesDataSource(dataStore, FakeSettingsDefaultsProvider(), SAFETY)
    }

    // ── Default state (no stored preferences) ────────────────────────────────

    @Test
    fun `fresh prefs - all default-enabled buttons are on`() =
        testScope.runTest {
            dataSource.enabledButtons.test {
                val enabled = awaitItem()
                val defaultOn = HomeButton.entries.filter { it.defaultEnabled }
                assertTrue(
                    "Expected all default-enabled buttons to be present",
                    enabled.containsAll(defaultOn),
                )
            }
        }

    @Test
    fun `fresh prefs - all optional buttons are off`() =
        testScope.runTest {
            dataSource.enabledButtons.test {
                val enabled = awaitItem()
                val optional = HomeButton.entries.filter { !it.defaultEnabled }
                assertTrue(
                    "Expected no optional buttons in enabled set",
                    optional.none { it in enabled },
                )
            }
        }

    // ── Default-enabled buttons (stored in DISABLED_BUTTONS) ─────────────────

    @Test
    fun `disabling a default-on button removes it from the enabled set`() =
        testScope.runTest {
            dataSource.setButtonEnabled(HomeButton.PHONE, false)

            dataSource.enabledButtons.test {
                assertFalse(HomeButton.PHONE in awaitItem())
            }
        }

    @Test
    fun `re-enabling a disabled default-on button restores it`() =
        testScope.runTest {
            dataSource.setButtonEnabled(HomeButton.PHONE, false)
            dataSource.setButtonEnabled(HomeButton.PHONE, true)

            dataSource.enabledButtons.test {
                assertTrue(HomeButton.PHONE in awaitItem())
            }
        }

    @Test
    fun `disabling one default-on button does not affect others`() =
        testScope.runTest {
            dataSource.setButtonEnabled(HomeButton.PHONE, false)

            dataSource.enabledButtons.test {
                val enabled = awaitItem()
                assertFalse(HomeButton.PHONE in enabled)
                assertTrue(HomeButton.CAMERA in enabled)
                assertTrue(HomeButton.FLASHLIGHT in enabled)
            }
        }

    // ── Optional buttons (stored in ENABLED_OPTIONAL_BUTTONS) ────────────────

    @Test
    fun `enabling an optional button adds it to the enabled set`() =
        testScope.runTest {
            dataSource.setButtonEnabled(HomeButton.CALCULATOR, true)

            dataSource.enabledButtons.test {
                assertTrue(HomeButton.CALCULATOR in awaitItem())
            }
        }

    @Test
    fun `disabling a previously enabled optional button removes it`() =
        testScope.runTest {
            dataSource.setButtonEnabled(HomeButton.CALCULATOR, true)
            dataSource.setButtonEnabled(HomeButton.CALCULATOR, false)

            dataSource.enabledButtons.test {
                assertFalse(HomeButton.CALCULATOR in awaitItem())
            }
        }

    @Test
    fun `enabling multiple optional buttons works independently`() =
        testScope.runTest {
            dataSource.setButtonEnabled(HomeButton.WEB, true)
            dataSource.setButtonEnabled(HomeButton.MAPS, true)

            dataSource.enabledButtons.test {
                val enabled = awaitItem()
                assertTrue(HomeButton.WEB in enabled)
                assertTrue(HomeButton.MAPS in enabled)
            }
        }

    // ── Cross-key isolation ───────────────────────────────────────────────────

    @Test
    fun `toggling an optional button does not affect default-on buttons`() =
        testScope.runTest {
            dataSource.setButtonEnabled(HomeButton.CALCULATOR, true)

            dataSource.enabledButtons.test {
                val enabled = awaitItem()
                val defaultOn = HomeButton.entries.filter { it.defaultEnabled }
                assertTrue(
                    "Enabling an optional button must not alter default-on buttons",
                    enabled.containsAll(defaultOn),
                )
            }
        }

    @Test
    fun `disabling a default-on button does not affect optional buttons`() =
        testScope.runTest {
            dataSource.setButtonEnabled(HomeButton.MAPS, true)
            dataSource.setButtonEnabled(HomeButton.PHONE, false)

            dataSource.enabledButtons.test {
                val enabled = awaitItem()
                assertTrue(HomeButton.MAPS in enabled)
                assertFalse(HomeButton.PHONE in enabled)
            }
        }

    // ── Live emission ─────────────────────────────────────────────────────────

    @Test
    fun `enabledButtons emits a new value each time a button is toggled`() =
        testScope.runTest {
            dataSource.enabledButtons.test {
                // Initial emission — all default-on, no optional
                val initial = awaitItem()
                assertFalse(HomeButton.WEB in initial)

                dataSource.setButtonEnabled(HomeButton.WEB, true)
                val afterEnable = awaitItem()
                assertTrue(HomeButton.WEB in afterEnable)

                dataSource.setButtonEnabled(HomeButton.WEB, false)
                val afterDisable = awaitItem()
                assertFalse(HomeButton.WEB in afterDisable)
            }
        }

    // ── Voice button ──────────────────────────────────────────────────────────

    @Test
    fun `fresh prefs - voice button is enabled by default`() =
        testScope.runTest {
            dataSource.voiceButtonEnabled.test {
                assertTrue(awaitItem())
            }
        }

    @Test
    fun `enabling voice button updates the preference`() =
        testScope.runTest {
            dataSource.setVoiceButtonEnabled(true)
            dataSource.voiceButtonEnabled.test {
                assertTrue(awaitItem())
            }
        }

    // ── SOS button ────────────────────────────────────────────────────────────

    @Test
    fun `fresh prefs - SOS button is enabled by default`() =
        testScope.runTest {
            dataSource.sosButtonEnabled.test {
                assertTrue(awaitItem())
            }
        }

    @Test
    fun `disabling SOS button updates the preference`() =
        testScope.runTest {
            dataSource.setSosButtonEnabled(false)
            dataSource.sosButtonEnabled.test {
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `re-enabling SOS button updates the preference`() =
        testScope.runTest {
            dataSource.setSosButtonEnabled(false)
            dataSource.setSosButtonEnabled(true)
            dataSource.sosButtonEnabled.test {
                assertTrue(awaitItem())
            }
        }

    @Test
    fun `sosButtonEnabled emits new values on change`() =
        testScope.runTest {
            dataSource.sosButtonEnabled.test {
                assertTrue(awaitItem())
                dataSource.setSosButtonEnabled(false)
                assertFalse(awaitItem())
                dataSource.setSosButtonEnabled(true)
                assertTrue(awaitItem())
            }
        }

    // ── High contrast ─────────────────────────────────────────────────────────

    @Test
    fun `fresh prefs - high contrast is disabled by default`() =
        testScope.runTest {
            dataSource.highContrastEnabled.test {
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `enabling high contrast updates the preference`() =
        testScope.runTest {
            dataSource.setHighContrastEnabled(true)
            dataSource.highContrastEnabled.test {
                assertTrue(awaitItem())
            }
        }

    @Test
    fun `disabling high contrast after enabling updates the preference`() =
        testScope.runTest {
            dataSource.setHighContrastEnabled(true)
            dataSource.setHighContrastEnabled(false)
            dataSource.highContrastEnabled.test {
                assertFalse(awaitItem())
            }
        }

    // ── resetToDefaults ───────────────────────────────────────────────────────

    @Test
    fun `resetToDefaults restores all preferences to their defaults`() =
        testScope.runTest {
            // Drive everything to non-default values
            dataSource.setButtonEnabled(HomeButton.PHONE, false)
            dataSource.setButtonEnabled(HomeButton.CALCULATOR, true) // optional
            dataSource.setVoiceButtonEnabled(true)
            dataSource.setSosButtonEnabled(false)
            dataSource.setHighContrastEnabled(true)

            dataSource.resetToDefaults()

            dataSource.enabledButtons.test {
                val enabled = awaitItem()
                assertTrue("PHONE should be restored", HomeButton.PHONE in enabled)
                assertFalse("CALCULATOR should be hidden again", HomeButton.CALCULATOR in enabled)
            }
            dataSource.voiceButtonEnabled.test {
                assertTrue(awaitItem())
            }
            dataSource.sosButtonEnabled.test {
                assertTrue(awaitItem())
            }
            dataSource.highContrastEnabled.test {
                assertFalse(awaitItem())
            }
        }

    // ── Remote-Config-driven defaults ────────────────────────────────────────

    @Test
    fun `overridden defaults change the fresh-install state`() =
        testScope.runTest {
            // Simulate Remote Config having flipped the factory defaults: SOS off,
            // voice on, high contrast on. With nothing stored yet, the source must
            // surface those instead of the hardcoded values.
            val overridden = SettingsDefaults.HARDCODED.copy(
                sosButtonVisible = false,
                voiceButtonVisible = true,
                highContrast = true,
            )
            val store = PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { tmpFolder.newFile("home_prefs_override.preferences_pb") },
            )
            val source = HomePreferencesDataSource(store, FakeSettingsDefaultsProvider(overridden), SAFETY)

            source.sosButtonEnabled.test { assertFalse(awaitItem()) }
            source.voiceButtonEnabled.test { assertTrue(awaitItem()) }
            source.highContrastEnabled.test { assertTrue(awaitItem()) }
        }

    @Test
    fun `a stored choice still wins over an overridden default`() =
        testScope.runTest {
            val overridden = SettingsDefaults.HARDCODED.copy(sosButtonVisible = false)
            val store = PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { tmpFolder.newFile("home_prefs_override2.preferences_pb") },
            )
            val source = HomePreferencesDataSource(store, FakeSettingsDefaultsProvider(overridden), SAFETY)

            // The user (or caregiver) explicitly turns SOS on; the default is irrelevant now.
            source.setSosButtonEnabled(true)
            source.sosButtonEnabled.test { assertTrue(awaitItem()) }
        }

    // ── Standard flavor (safety features compiled out) ────────────────────────

    @Test
    fun `standard flavor - sos stays hidden even when stored and defaulted on`() =
        testScope.runTest {
            // Remote Config says the SOS button should be visible…
            val overridden = SettingsDefaults.HARDCODED.copy(
                sosButtonVisible = true,
                voiceButtonVisible = true,
            )
            val store = PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { tmpFolder.newFile("home_prefs_standard.preferences_pb") },
            )
            val source =
                HomePreferencesDataSource(store, FakeSettingsDefaultsProvider(overridden), STANDARD)

            // …and the user has explicitly turned it on (e.g. prefs written by a
            // previous safety-flavor install). The manifest has no SMS permission,
            // so the button must stay hidden regardless.
            source.setSosButtonEnabled(true)
            source.sosButtonEnabled.test { assertFalse(awaitItem()) }

            // Voice is NOT safety-gated — it ships in both flavors.
            source.setVoiceButtonEnabled(true)
            source.voiceButtonEnabled.test { assertTrue(awaitItem()) }
        }
}
