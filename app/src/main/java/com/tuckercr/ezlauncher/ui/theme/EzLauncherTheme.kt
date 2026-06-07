package com.tuckercr.ezlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorPhone = Color(0xFF839B3B)
val ColorText = Color(0xFF738437)
val ColorCamera = Color(0xFF197C72)
val ColorMagnifier = Color(0xFFD0942B)
val ColorAllApps = Color(0xFF565989)
val ColorFlashlight = Color(0xFFDAB129)
val ColorSos = Color(0xFFDE3226)
val ColorSpeedDial = Color(0xFF3C6478)
val ColorWeb = Color(0xFF1565C0)
val ColorMaps = Color(0xFF2E7D32)
val ColorEmail = Color(0xFF6D4C41)
val ColorPhotos = Color(0xFF6A1B9A)
val ColorYouTube = Color(0xFFC62828)
val ColorCalculator = Color(0xFF00695C)

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

@Composable
fun EzLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
