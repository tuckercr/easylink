package com.fangjet.launcher.domain

import com.fangjet.launcher.data.apps.FavoriteAppsMode
import com.fangjet.launcher.domain.model.AppInfo
import com.fangjet.shared.config.SettingsDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteAppsSelectorTest {

    private fun app(
        pkg: String,
        label: String = pkg.substringAfterLast('.'),
    ) = AppInfo(packageName = pkg, label = label, icon = null)

    private val spotify = app("com.spotify.music", "Spotify")
    private val audible = app("com.audible.application", "Audible")
    private val maps = app("com.google.maps", "Maps")
    private val chess = app("com.chess", "Chess")
    private val installed = listOf(audible, chess, maps, spotify)

    // The production cold-start pool (Spotify + Audible are on it; Chess and the
    // fake maps package are not).
    private val pool = SettingsDefaults.HARDCODED.favoriteAppsCuratedPool

    // ── AUTOMATIC ────────────────────────────────────────────────────────────

    @Test
    fun `automatic ranks by launch count descending`() {
        val result = FavoriteAppsSelector.select(
            installed = installed,
            launchCounts = mapOf(
                spotify.packageName to 10L,
                maps.packageName to 3L,
                audible.packageName to 7L,
            ),
            mode = FavoriteAppsMode.AUTOMATIC,
            customPackages = emptyList(),
            max = 3,
            curatedPool = pool,
        )
        assertEquals(listOf(spotify, audible, maps), result)
    }

    @Test
    fun `automatic with no history fills by curated priority not the alphabet`() {
        // Spotify and Audible are on the curated list; Chess and this fake
        // maps package are not — so the row starts with the curated picks even
        // though "Audible" and "Chess" sort earlier alphabetically.
        val result = FavoriteAppsSelector.select(
            installed = installed,
            launchCounts = emptyMap(),
            mode = FavoriteAppsMode.AUTOMATIC,
            customPackages = emptyList(),
            max = 2,
            curatedPool = pool,
        )
        assertEquals(listOf(spotify, audible), result)
    }

    @Test
    fun `uncurated apps still fill after curated ones, alphabetically`() {
        val result = FavoriteAppsSelector.select(
            installed = installed,
            launchCounts = emptyMap(),
            mode = FavoriteAppsMode.AUTOMATIC,
            customPackages = emptyList(),
            max = 4,
            curatedPool = pool,
        )
        assertEquals(listOf(spotify, audible, chess, maps), result)
    }

    @Test
    fun `automatic mixes used apps first then curated fill`() {
        val result = FavoriteAppsSelector.select(
            installed = installed,
            launchCounts = mapOf(maps.packageName to 1L),
            mode = FavoriteAppsMode.AUTOMATIC,
            customPackages = emptyList(),
            max = 3,
            curatedPool = pool,
        )
        assertEquals(listOf(maps, spotify, audible), result)
    }

    @Test
    fun `own apps never occupy an automatic slot`() {
        val care = app("com.fangjet.care", "EasyLink Care")
        val result = FavoriteAppsSelector.select(
            installed = installed + care,
            launchCounts = mapOf(care.packageName to 99L),
            mode = FavoriteAppsMode.AUTOMATIC,
            customPackages = emptyList(),
            max = 12,
            curatedPool = pool,
        )
        assertTrue(result.none { it.packageName.startsWith("com.fangjet.") })
    }

    @Test
    fun `an explicit custom pick of an own app is honoured`() {
        val care = app("com.fangjet.care", "EasyLink Care")
        val result = FavoriteAppsSelector.select(
            installed = installed + care,
            launchCounts = emptyMap(),
            mode = FavoriteAppsMode.CUSTOM,
            customPackages = listOf(care.packageName),
            max = 12,
            curatedPool = pool,
        )
        assertEquals(listOf(care), result)
    }

    @Test
    fun `ties break alphabetically for a stable row`() {
        val result = FavoriteAppsSelector.select(
            installed = installed,
            launchCounts = mapOf(spotify.packageName to 5L, audible.packageName to 5L),
            mode = FavoriteAppsMode.AUTOMATIC,
            customPackages = emptyList(),
            max = 2,
            curatedPool = pool,
        )
        assertEquals(listOf(audible, spotify), result)
    }

    // ── CUSTOM ───────────────────────────────────────────────────────────────

    @Test
    fun `custom preserves pick order and ignores counts`() {
        val result = FavoriteAppsSelector.select(
            installed = installed,
            launchCounts = mapOf(audible.packageName to 100L),
            mode = FavoriteAppsMode.CUSTOM,
            customPackages = listOf(maps.packageName, spotify.packageName),
            max = 12,
            curatedPool = pool,
        )
        assertEquals(listOf(maps, spotify), result)
    }

    @Test
    fun `custom skips uninstalled packages instead of crashing`() {
        val result = FavoriteAppsSelector.select(
            installed = installed,
            launchCounts = emptyMap(),
            mode = FavoriteAppsMode.CUSTOM,
            customPackages = listOf("com.gone.app", spotify.packageName),
            max = 12,
            curatedPool = pool,
        )
        assertEquals(listOf(spotify), result)
    }

    // ── Curated pool (Remote Config-tunable) ─────────────────────────────────

    @Test
    fun `a server-supplied pool reorders the cold-start fill`() {
        val result = FavoriteAppsSelector.select(
            installed = installed,
            launchCounts = emptyMap(),
            mode = FavoriteAppsMode.AUTOMATIC,
            customPackages = emptyList(),
            max = 2,
            curatedPool = listOf(chess.packageName, maps.packageName),
        )
        assertEquals(listOf(chess, maps), result)
    }

    @Test
    fun `pool entries not installed on the device are skipped`() {
        val result = FavoriteAppsSelector.select(
            installed = installed,
            launchCounts = emptyMap(),
            mode = FavoriteAppsMode.AUTOMATIC,
            customPackages = emptyList(),
            max = 2,
            curatedPool = listOf("com.not.installed", chess.packageName),
        )
        assertEquals(listOf(chess, audible), result)
    }

    @Test
    fun `respects the max cap in both modes`() {
        FavoriteAppsMode.entries.forEach { mode ->
            val result = FavoriteAppsSelector.select(
                installed = installed,
                launchCounts = emptyMap(),
                mode = mode,
                customPackages = installed.map { it.packageName },
                max = 2,
                curatedPool = pool,
            )
            assertTrue("$mode should cap at 2", result.size <= 2)
        }
    }
}
