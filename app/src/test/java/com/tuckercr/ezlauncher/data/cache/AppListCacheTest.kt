package com.tuckercr.ezlauncher.data.cache

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AppListCache.queryApps].
 *
 * [queryApps] issues a single [PackageManager.queryIntentActivities] call with
 * [Intent.ACTION_MAIN] + [PackageManager.GET_RESOLVED_FILTER], then inspects
 * each result's [ResolveInfo.filter] to classify it as LAUNCHER or HOME.
 *
 * Test strategy:
 * - All [ResolveInfo] objects the PM returns are supplied via [givenAllMainActivities].
 * - Fields on [ResolveInfo] (e.g. [ResolveInfo.filter], [ResolveInfo.activityInfo])
 *   are public Java fields — they cannot be intercepted by MockK. We use [spyk]
 *   so the real object's fields are accessible while methods like [ResolveInfo.loadLabel]
 *   can still be stubbed.
 * - [IntentFilter.hasCategory] is stubbed via a MockK mock so it works without
 *   Robolectric (Android stubs return default values, not real behaviour).
 *
 * Note: [AppListCache] is a Kotlin `object` (process-lifetime singleton).
 * Its [AppListCache.prewarm] guard cannot be reset between tests; the prewarm
 * idempotency is therefore tested via observable side-effects only.
 */
class AppListCacheTest {

    private val hostPackage = "com.tuckercr.ezlauncher"

    private lateinit var context: Context
    private lateinit var pm: PackageManager

    @Before
    fun setup() {
        pm = mockk(relaxed = true)
        context = mockk {
            every { packageManager } returns pm
            every { packageName } returns hostPackage
        }

        // Default: no apps installed
        every { pm.queryIntentActivities(any(), any<Int>()) } returns emptyList()
    }

    // ── queryApps filtering ───────────────────────────────────────────────────

    @Test
    fun `queryApps returns empty list when package manager returns nothing`() =
        runTest {
            val result = AppListCache.queryApps(context)
            assertTrue(result.isEmpty())
        }

    @Test
    fun `queryApps excludes the host package`() =
        runTest {
            givenAllMainActivities(
                launcherApp(pkg = hostPackage, label = "EZ Launcher"),
                launcherApp(pkg = "com.example.other", label = "Other App"),
            )

            val result = AppListCache.queryApps(context)

            assertTrue(result.none { it.packageName == hostPackage })
            assertEquals(1, result.size)
            assertEquals("com.example.other", result.first().packageName)
        }

    @Test
    fun `queryApps excludes packages that declare CATEGORY_HOME`() =
        runTest {
            // Rival launcher appears twice — once as a HOME screen, once as a LAUNCHER entry
            givenAllMainActivities(
                homeApp(pkg = "com.example.rival.launcher"),
                launcherApp(pkg = "com.example.rival.launcher", label = "Rival Launcher"),
                launcherApp(pkg = "com.example.notes", label = "Notes"),
            )

            val result = AppListCache.queryApps(context)

            assertEquals(1, result.size)
            assertEquals("com.example.notes", result.first().packageName)
        }

    @Test
    fun `queryApps deduplicates apps with multiple launcher activities`() =
        runTest {
            // Simulate an app with two CATEGORY_LAUNCHER activities (e.g. Big Launcher)
            givenAllMainActivities(
                launcherApp(pkg = "com.example.multi", label = "Multi"),
                launcherApp(pkg = "com.example.multi", label = "Multi"),
                launcherApp(pkg = "com.example.single", label = "Single"),
            )

            val result = AppListCache.queryApps(context)

            assertEquals(2, result.size)
            assertEquals(1, result.count { it.packageName == "com.example.multi" })
        }

    @Test
    fun `queryApps returns apps sorted alphabetically by label`() =
        runTest {
            givenAllMainActivities(
                launcherApp(pkg = "com.example.zebra", label = "Zebra"),
                launcherApp(pkg = "com.example.apple", label = "Apple"),
                launcherApp(pkg = "com.example.mango", label = "Mango"),
            )

            val result = AppListCache.queryApps(context)

            assertEquals(listOf("Apple", "Mango", "Zebra"), result.map { it.label })
        }

    @Test
    fun `queryApps excludes host and home apps while keeping regular apps`() =
        runTest {
            givenAllMainActivities(
                launcherApp(pkg = hostPackage, label = "EZ Launcher"),
                homeApp(pkg = "com.big.launcher.demo"),
                launcherApp(pkg = "com.big.launcher.demo", label = "Big Launcher"),
                launcherApp(pkg = "com.example.camera", label = "Camera"),
                launcherApp(pkg = "com.example.maps", label = "Maps"),
            )

            val result = AppListCache.queryApps(context)

            assertEquals(2, result.size)
            val packages = result.map { it.packageName }
            assertTrue("com.example.camera" in packages)
            assertTrue("com.example.maps" in packages)
            assertFalse(hostPackage in packages)
            assertFalse("com.big.launcher.demo" in packages)
        }

    @Test
    fun `queryApps falls back to package name if loadLabel throws`() =
        runTest {
            val brokenApp = launcherApp(pkg = "com.broken.label", label = "Irrelevant")
            // Simulate loadLabel throwing (e.g. package just uninstalled/broken during query)
            every { brokenApp.loadLabel(any()) } throws RuntimeException("Package Manager error")

            givenAllMainActivities(brokenApp)

            val result = AppListCache.queryApps(context)

            assertEquals(1, result.size)
            assertEquals("com.broken.label", result.first().label)
        }

    // ── prewarm idempotency ───────────────────────────────────────────────────

    @Test
    fun `apps StateFlow is non-null after prewarm`() =
        runTest {
            // AppListCache is a singleton — prewarm may already have been called by a
            // prior test. We can only assert apps.value is accessible without throwing.
            AppListCache.prewarm(context)
            // StateFlow.value is always non-null; we just confirm the call doesn't throw.
            @Suppress("USELESS_CAST")
            assertNotNull("Expected apps list to be accessible", AppListCache.apps.value as Any?)
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Configures the mock PM to return [apps] for any queryIntentActivities call.
     * The new single-query architecture sends one [Intent.ACTION_MAIN] intent and
     * classifies results by their [ResolveInfo.filter] categories in memory.
     */
    private fun givenAllMainActivities(vararg apps: ResolveInfo) {
        every { pm.queryIntentActivities(any(), any<Int>()) } returns apps.toList()
    }

    /**
     * A [ResolveInfo] whose filter declares [Intent.CATEGORY_LAUNCHER] only.
     *
     * Uses [spyk] so public Java fields ([ResolveInfo.activityInfo], [ResolveInfo.filter])
     * can be set directly while methods ([ResolveInfo.loadLabel]) are still stubbable.
     * [IntentFilter] itself is mocked so [IntentFilter.hasCategory] works without Robolectric.
     */
    private fun launcherApp(
        pkg: String,
        label: String,
    ): ResolveInfo =
        spyk(ResolveInfo()).apply {
            activityInfo = ActivityInfo().apply { packageName = pkg }
            filter = mockk<IntentFilter> {
                every { hasCategory(Intent.CATEGORY_LAUNCHER) } returns true
                every { hasCategory(Intent.CATEGORY_HOME) } returns false
            }
            every { loadLabel(any()) } returns label
            every { loadIcon(any()) } returns mockk(relaxed = true)
        }

    /**
     * A [ResolveInfo] whose filter declares [Intent.CATEGORY_HOME] (i.e. a launcher).
     * These should be excluded from the All Apps grid.
     */
    private fun homeApp(pkg: String): ResolveInfo =
        spyk(ResolveInfo()).apply {
            activityInfo = ActivityInfo().apply { packageName = pkg }
            filter = mockk<IntentFilter> {
                every { hasCategory(Intent.CATEGORY_LAUNCHER) } returns false
                every { hasCategory(Intent.CATEGORY_HOME) } returns true
            }
            every { loadLabel(any()) } returns ""
            every { loadIcon(any()) } returns mockk(relaxed = true)
        }
}
