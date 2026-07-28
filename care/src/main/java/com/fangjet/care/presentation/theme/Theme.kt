package com.fangjet.care.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * EasyLink brand palette, shared with the pitch deck and easylinkcare.com.
 *
 * Note this is the *caregiver's* theme, not the elder's: normal type scale,
 * normal touch targets. The oversized-everything treatment belongs to the
 * launcher, where it serves a real accessibility need.
 */
val Teal = Color(0xFF12776A)
val TealDeep = Color(0xFF0E3A3E)
val Orange = Color(0xFFDD5A1E)
val Ground = Color(0xFFFAF8F3)
val Ink = Color(0xFF17231F)

val Critical = Color(0xFFC0392B)
val Warning = Color(0xFFB7791F)
val Good = Color(0xFF2E7D46)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Orange,
    onSecondary = Color.White,
    background = Ground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    error = Critical,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3FA294),
    onPrimary = Color.White,
    secondary = Orange,
    onSecondary = Color.White,
    background = Color(0xFF12181A),
    onBackground = Color(0xFFECF2F0),
    surface = Color(0xFF182022),
    onSurface = Color(0xFFECF2F0),
    error = Color(0xFFE57368),
)

@Composable
fun EasyLinkCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
