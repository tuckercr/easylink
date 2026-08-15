package com.fangjet.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val ColorPhone = Color(0xFF657D21)
val ColorText = Color(0xFF2F65A7)
val ColorCamera = Color(0xFF236B63)

val ColorMagnifier = Color(0xFF986315)
val ColorAllApps = Color(0xFF565989)
val ColorFlashlight = Color(0xFF927000)

val ColorSpeedDial = Color(0xFF3C6478)
val ColorMeds = Color(0xFF0A6F9E)

val ColorWeb = Color(0xFF28773F)
val ColorFacebook = Color(0xFF1859A9)
val ColorMaps = Color(0xFF2E6F3D)

val ColorEmail = Color(0xFF795548)
val ColorPhotos = Color(0xFF71378F)
val ColorYouTube = Color(0xFF803B3B)

val ColorCalculator = Color(0xFF0E665D)

// Not a home-tile colour: used by the safety flavor's SOS button and
// countdown screen. Red stays reserved for emergency UI.
val ColorSos = Color(0xFFC8322D)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF004788),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF003910),
    background = Color(0xFF111111),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF690005),
)

private val HighContrastColors = darkColorScheme(
    primary = Color(0xFFB0D9FF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF004788),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFC8F5CA),
    onSecondary = Color(0xFF003910),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFCCCCCC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** True when the user has enabled high-contrast mode in Settings. */
val LocalHighContrast = compositionLocalOf { false }

@Composable
fun EasyLinkTheme(
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalHighContrast provides highContrast) {
        MaterialTheme(
            colorScheme = if (highContrast) HighContrastColors else DarkColors,
            content = content,
        )
    }
}
