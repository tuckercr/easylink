package com.fangjet.launcher.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.fangjet.launcher.domain.model.FallSensitivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class FallDetectionPreferencesTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var prefs: FallDetectionPreferences

    @Before
    fun setup() {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("fall_prefs_test.preferences_pb") },
        )
        prefs = FallDetectionPreferences(dataStore)
    }

    // ── Defaults ──────────────────────────────────────────────────────────────

    @Test
    fun `fall detection is disabled by default`() =
        testScope.runTest {
            prefs.isEnabled.test {
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `sensitivity defaults to MEDIUM`() =
        testScope.runTest {
            prefs.sensitivity.test {
                assertEquals(FallSensitivity.MEDIUM, awaitItem())
            }
        }

    // ── Enable / disable ──────────────────────────────────────────────────────

    @Test
    fun `enabling fall detection persists to true`() =
        testScope.runTest {
            prefs.setEnabled(true)
            prefs.isEnabled.test {
                assertTrue(awaitItem())
            }
        }

    @Test
    fun `disabling fall detection persists to false`() =
        testScope.runTest {
            prefs.setEnabled(true)
            prefs.setEnabled(false)
            prefs.isEnabled.test {
                assertFalse(awaitItem())
            }
        }

    // ── Sensitivity ───────────────────────────────────────────────────────────

    @Test
    fun `setting sensitivity to LOW persists`() =
        testScope.runTest {
            prefs.setSensitivity(FallSensitivity.LOW)
            prefs.sensitivity.test {
                assertEquals(FallSensitivity.LOW, awaitItem())
            }
        }

    @Test
    fun `setting sensitivity to HIGH persists`() =
        testScope.runTest {
            prefs.setSensitivity(FallSensitivity.HIGH)
            prefs.sensitivity.test {
                assertEquals(FallSensitivity.HIGH, awaitItem())
            }
        }

    @Test
    fun `sensitivity can be changed multiple times`() =
        testScope.runTest {
            prefs.setSensitivity(FallSensitivity.LOW)
            prefs.setSensitivity(FallSensitivity.HIGH)
            prefs.sensitivity.test {
                assertEquals(FallSensitivity.HIGH, awaitItem())
            }
        }

    // ── Live emission ─────────────────────────────────────────────────────────

    @Test
    fun `isEnabled emits a new value each time it changes`() =
        testScope.runTest {
            prefs.isEnabled.test {
                assertFalse(awaitItem())
                prefs.setEnabled(true)
                assertTrue(awaitItem())
                prefs.setEnabled(false)
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `sensitivity emits a new value each time it changes`() =
        testScope.runTest {
            prefs.sensitivity.test {
                assertEquals(FallSensitivity.MEDIUM, awaitItem())
                prefs.setSensitivity(FallSensitivity.HIGH)
                assertEquals(FallSensitivity.HIGH, awaitItem())
                prefs.setSensitivity(FallSensitivity.LOW)
                assertEquals(FallSensitivity.LOW, awaitItem())
            }
        }
}
