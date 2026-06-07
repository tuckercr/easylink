package com.tuckercr.ezlauncher.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.tuckercr.ezlauncher.domain.model.HomeButton
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
        dataSource = HomePreferencesDataSource(dataStore)
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
}
