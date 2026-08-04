package com.fangjet.launcher.presentation.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.MediaStore
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.R
import com.fangjet.launcher.domain.model.HomeButton
import com.fangjet.launcher.presentation.tts.TtsViewModel
import com.fangjet.launcher.presentation.voice.VoiceCommandViewModel
import com.fangjet.launcher.presentation.voice.VoiceEffect
import com.fangjet.launcher.presentation.voice.VoiceOverlay
import com.fangjet.launcher.presentation.voice.VoiceUiState
import com.fangjet.launcher.ui.theme.ColorAllApps
import com.fangjet.launcher.ui.theme.ColorCalculator
import com.fangjet.launcher.ui.theme.ColorCamera
import com.fangjet.launcher.ui.theme.ColorEmail
import com.fangjet.launcher.ui.theme.ColorFlashlight
import com.fangjet.launcher.ui.theme.ColorMagnifier
import com.fangjet.launcher.ui.theme.ColorMaps
import com.fangjet.launcher.ui.theme.ColorMeds
import com.fangjet.launcher.ui.theme.ColorPhone
import com.fangjet.launcher.ui.theme.ColorPhotos
import com.fangjet.launcher.ui.theme.ColorSos
import com.fangjet.launcher.ui.theme.ColorSpeedDial
import com.fangjet.launcher.ui.theme.ColorText
import com.fangjet.launcher.ui.theme.ColorWeb
import com.fangjet.launcher.ui.theme.ColorYouTube
import com.fangjet.launcher.ui.theme.LocalHighContrast
import com.fangjet.weather.model.WeatherInfo
import kotlinx.coroutines.launch

private const val BUTTONS_PER_ROW = 3

private val ColorVoice = Color(0xFF5C6BC0) // indigo

/**
 * Swipe up anywhere on the home screen to open All Apps (Pixel-launcher style).
 * Taps still reach the buttons — a drag only fires after the touch slop, and the
 * home screen isn't vertically scrollable, so nothing competes with the gesture.
 */
private fun Modifier.swipeUpToOpen(onSwipeUp: () -> Unit): Modifier =
    pointerInput(Unit) {
        var dragTotal = 0f
        val thresholdPx = 90.dp.toPx()
        detectVerticalDragGestures(
            onDragStart = { dragTotal = 0f },
            onDragCancel = { dragTotal = 0f },
            onDragEnd = { if (dragTotal <= -thresholdPx) onSwipeUp() },
        ) { change, dragAmount ->
            dragTotal += dragAmount
            change.consume()
        }
    }

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    voiceViewModel: VoiceCommandViewModel = hiltViewModel(),
    ttsViewModel: TtsViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner,
    ),
    onNavigateToApps: () -> Unit,
    onNavigateToMagnifier: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToForecast: () -> Unit,
    onNavigateToSpeedDial: () -> Unit,
    onNavigateToMedications: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceState by voiceViewModel.uiState.collectAsStateWithLifecycle()

    // Flashlight torch control — only binds camera when torch is on
    val flashlightOn = (state as? HomeUiState.Success)?.isFlashlightOn ?: false
    FlashlightEffect(enabled = flashlightOn)

    // ── Consume one-shot voice effects ────────────────────────────────────────
    LaunchedEffect(voiceViewModel) {
        voiceViewModel.effects.collect { effect ->
            when (effect) {
                is VoiceEffect.NavigateToApps -> onNavigateToApps()
                is VoiceEffect.NavigateHome -> voiceViewModel.dismiss() // already home
                is VoiceEffect.FlashlightOn -> viewModel.setFlashlightEnabled(true)
                is VoiceEffect.FlashlightOff -> viewModel.setFlashlightEnabled(false)
                is VoiceEffect.ToggleFlashlight -> viewModel.toggleFlashlight()
            }
        }
    }

    // Location permission launcher — called when user taps the weather card
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.refreshWeather() }

    // RECORD_AUDIO permission launcher — called when user taps the mic button
    val context = LocalContext.current
    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) voiceViewModel.startListening() }

    val onMicTapped: () -> Unit = {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasAudio) {
            voiceViewModel.startListening()
        } else {
            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Root Box allows overlay on top of screen content ─────────────────────
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .swipeUpToOpen(onSwipeUp = onNavigateToApps)
                .padding(16.dp),
        ) {
            when (val s = state) {
                is HomeUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is HomeUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is HomeUiState.Success -> {
                    // ── Weather card + Settings ─────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WeatherCard(
                            weather = s.weather,
                            onRequestPermission = {
                                locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            },
                            onRefresh = { viewModel.refreshWeather() },
                            onNavigateToForecast = onNavigateToForecast,
                            modifier = Modifier.weight(1f),
                        )
                        SettingsButton(onClick = onNavigateToSettings)
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Apps Row (real apps, real icons, usage-ranked) ──────
                    val favoriteApps by viewModel.favoriteApps.collectAsStateWithLifecycle()
                    val badgedPackages by viewModel.badgedPackages.collectAsStateWithLifecycle()
                    if (favoriteApps.isNotEmpty()) {
                        FavoriteAppsRow(
                            apps = favoriteApps,
                            badgedPackages = badgedPackages,
                            onAppTapped = { viewModel.onAppTapped(it) },
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Dynamic button grid ─────────────────────────────────
                    val view = LocalView.current
                    val launchIntent: (Intent) -> Unit = { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            context.startActivity(intent)
                        } catch (_: android.content.ActivityNotFoundException) {
                            // No app handles this intent — silently ignore
                        }
                    }

                    val flashlightOnText = stringResource(R.string.flashlight_on)
                    val flashlightOffText = stringResource(R.string.flashlight_off)

                    ButtonGrid(
                        modifier = Modifier.weight(1f),
                        enabledButtons = s.enabledButtons,
                        isFlashlightOn = s.isFlashlightOn,
                        onPhone = { launchIntent(Intent(Intent.ACTION_DIAL)) },
                        onText = {
                            launchIntent(
                                Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                                },
                            )
                        },
                        onCamera = { launchIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
                        onMagnifier = onNavigateToMagnifier,
                        onAllApps = onNavigateToApps,
                        onSpeedDial = onNavigateToSpeedDial,
                        onMedications = onNavigateToMedications,
                        onFlashlight = { viewModel.toggleFlashlight() },
                        onWeb = {
                            // Try Chrome variants first (restores last session without a URL).
                            // Fall back to ACTION_VIEW on a URL — this opens any installed
                            // browser (Firefox, Brave, etc.) and is universally supported.
                            val intent =
                                context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                                    ?: context.packageManager.getLaunchIntentForPackage("com.chrome.beta")
                                    ?: context.packageManager.getLaunchIntentForPackage("com.chrome.dev")
                                    ?: Intent(Intent.ACTION_VIEW, "https://www.google.com".toUri())
                            launchIntent(intent)
                        },
                        onMaps = { launchIntent(Intent(Intent.ACTION_VIEW, "geo:0,0".toUri())) },
                        onEmail = {
                            launchIntent(
                                Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_EMAIL)
                                },
                            )
                        },
                        onPhotos = {
                            launchIntent(
                                Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_GALLERY)
                                },
                            )
                        },
                        onYouTube = {
                            launchIntent(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://www.youtube.com".toUri(),
                                ),
                            )
                        },
                        onCalculator = {
                            launchIntent(
                                Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_CALCULATOR)
                                },
                            )
                        },
                        onLongPress = { label ->
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            ttsViewModel.speak(label)
                        },
                        flashlightOnText = flashlightOnText,
                        flashlightOffText = flashlightOffText,
                    )

                    // ── Voice command button (only when enabled in Settings) ──
                    if (s.voiceButtonEnabled) {
                        Spacer(Modifier.height(12.dp))

                        Surface(
                            onClick = onMicTapped,
                            color = ColorVoice,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_mic),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp),
                                )
                                Spacer(Modifier.size(10.dp))
                                Text(
                                    stringResource(R.string.home_voice_command_button),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    // ── SOS (only when enabled in Settings) ──────────────────
                    if (s.sosButtonEnabled) {
                        // 12 dp gap above SOS — same rhythm as the button grid gaps,
                        // present whether or not the voice button is visible.
                        Spacer(Modifier.height(12.dp))

                        SosHoldButton(
                            onActivate = onNavigateToSos,
                            holdDurationMs = s.sosHoldDurationMs.toInt(),
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // ── Voice overlay (rendered on top when active) ───────────────────────
        if (voiceState !is VoiceUiState.Idle) {
            VoiceOverlay(
                state = voiceState,
                onDismiss = { voiceViewModel.dismiss() },
                onRetry = { voiceViewModel.retry() },
            )
        }
    }
}

// ── Weather card ──────────────────────────────────────────────────────────────

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = stringResource(R.string.home_settings_desc),
            tint = Color.White,
            modifier = Modifier.size(34.dp),
        )
    }
}

// ── SOS hold-to-activate button ───────────────────────────────────────────────

/**
 * SOS requires a deliberate press-and-hold so a stray tap can't trigger it. A
 * white sweep fills the button while it's held; releasing early cancels and the
 * fill drains back. Completing the hold gives a haptic tick and opens the SOS
 * countdown screen (which still offers a big Cancel).
 *
 * [holdDurationMs] is a Remote Config-tunable default (see [com.fangjet.shared.config.SettingsDefaults]),
 * clamped to a safe range before it reaches here.
 */
@Composable
private fun SosHoldButton(
    onActivate: () -> Unit,
    holdDurationMs: Int,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ColorSos)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        var completed = false
                        val holdJob = scope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(holdDurationMs, easing = LinearEasing),
                            )
                            completed = true
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onActivate()
                            progress.snapTo(0f)
                        }
                        tryAwaitRelease()
                        if (!completed) {
                            holdJob.cancel()
                            scope.launch { progress.animateTo(0f, tween(250)) }
                        }
                    },
                )
            },
    ) {
        // Progress sweep — fills left to right over the 3-second hold
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(Color.White.copy(alpha = 0.30f)),
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.sos),
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 6.sp,
            )
            Text(
                stringResource(R.string.sos_hold_hint),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun WeatherCard(
    weather: WeatherInfo,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToForecast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White.copy(alpha = 0.10f))
        .padding(horizontal = 16.dp, vertical = 10.dp)

    when (weather) {
        is WeatherInfo.Loading -> {
            Row(
                modifier = cardModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    stringResource(R.string.weather_loading),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
            }
        }

        is WeatherInfo.Available -> {
            Row(
                modifier = cardModifier.clickable(onClick = onNavigateToForecast),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(weather.emoji, fontSize = 28.sp)
                    Column {
                        Text(
                            text = weather.displayTemp,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        val subtitle = weather.city?.let { "$it · ${weather.description}" }
                            ?: weather.description
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                        )
                    }
                }
                // Chevron hint — tapping opens forecast
                Text("›", color = Color.White.copy(alpha = 0.5f), fontSize = 22.sp)
            }
        }

        is WeatherInfo.PermissionNeeded -> {
            Row(
                modifier = cardModifier.clickable(onClick = onRequestPermission),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🌤️", fontSize = 22.sp)
                Text(
                    stringResource(R.string.weather_tap_to_enable),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
            }
        }

        is WeatherInfo.LocationDisabled -> {
            val ctx = LocalContext.current
            Row(
                modifier = cardModifier.clickable {
                    ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("📍", fontSize = 22.sp)
                Column {
                    Text(
                        stringResource(R.string.weather_location_off),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(R.string.weather_tap_location_settings),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        is WeatherInfo.Unavailable -> {
            Row(
                modifier = cardModifier.clickable(onClick = onRefresh),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🌡️", fontSize = 22.sp)
                Text(
                    stringResource(R.string.weather_unavailable),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                )
            }
        }
    }
}

// ── Dynamic button grid ───────────────────────────────────────────────────────

@Composable
private fun ButtonGrid(
    enabledButtons: List<HomeButton>,
    isFlashlightOn: Boolean,
    onPhone: () -> Unit,
    onText: () -> Unit,
    onCamera: () -> Unit,
    onMagnifier: () -> Unit,
    onAllApps: () -> Unit,
    onSpeedDial: () -> Unit,
    onMedications: () -> Unit,
    onFlashlight: () -> Unit,
    onWeb: () -> Unit,
    onMaps: () -> Unit,
    onEmail: () -> Unit,
    onPhotos: () -> Unit,
    onYouTube: () -> Unit,
    onCalculator: () -> Unit,
    onLongPress: (label: String) -> Unit,
    flashlightOnText: String,
    flashlightOffText: String,
    modifier: Modifier = Modifier,
) {
    if (enabledButtons.isEmpty()) return

    // Cap font scale so button labels stay legible at max accessibility settings.
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale.coerceAtMost(1.3f),
        ),
    ) {
        // modifier carries weight(1f) from the parent Column so the grid expands
        // to fill all available vertical space between the weather card and the
        // voice/SOS buttons. Each row then takes an equal share of that space.
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            enabledButtons.chunked(BUTTONS_PER_ROW).forEach { rowButtons ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowButtons.forEach { button ->
                        SingleHomeButton(
                            button = button,
                            isFlashlightOn = isFlashlightOn,
                            onPhone = onPhone,
                            onText = onText,
                            onCamera = onCamera,
                            onMagnifier = onMagnifier,
                            onAllApps = onAllApps,
                            onSpeedDial = onSpeedDial,
                            onMedications = onMedications,
                            onFlashlight = onFlashlight,
                            onWeb = onWeb,
                            onMaps = onMaps,
                            onEmail = onEmail,
                            onPhotos = onPhotos,
                            onYouTube = onYouTube,
                            onCalculator = onCalculator,
                            onLongPress = onLongPress,
                            flashlightOnText = flashlightOnText,
                            flashlightOffText = flashlightOffText,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Fill empty slots so partial rows keep consistent button widths
                    repeat(BUTTONS_PER_ROW - rowButtons.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    } // end CompositionLocalProvider
}

@Composable
private fun SingleHomeButton(
    button: HomeButton,
    isFlashlightOn: Boolean,
    onPhone: () -> Unit,
    onText: () -> Unit,
    onCamera: () -> Unit,
    onMagnifier: () -> Unit,
    onAllApps: () -> Unit,
    onSpeedDial: () -> Unit,
    onMedications: () -> Unit,
    onFlashlight: () -> Unit,
    onWeb: () -> Unit,
    onMaps: () -> Unit,
    onEmail: () -> Unit,
    onPhotos: () -> Unit,
    onYouTube: () -> Unit,
    onCalculator: () -> Unit,
    onLongPress: (label: String) -> Unit,
    flashlightOnText: String,
    flashlightOffText: String,
    modifier: Modifier,
) {
    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    when (button) {
        HomeButton.PHONE -> {
            val label = stringResource(R.string.phone)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_call,
                color = ColorPhone,
                modifier = modifier,
                onClick = onPhone,
                onLongClick = { onLongPress(label) },
            )
        }

        HomeButton.TEXT -> {
            val label = stringResource(R.string.sms)
            val ttsText = stringResource(R.string.home_tts_text_messages)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_sms,
                color = ColorText,
                modifier = modifier,
                onClick = onText,
                onLongClick = { onLongPress(ttsText) },
            )
        }

        HomeButton.CAMERA -> {
            val label = stringResource(R.string.camera)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_camera,
                color = ColorCamera,
                modifier = modifier,
                onClick = onCamera,
                onLongClick = { onLongPress(label) },
            )
        }

        HomeButton.MAGNIFIER -> {
            val label = stringResource(R.string.magnifier)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_magnifier,
                color = ColorMagnifier,
                modifier = modifier,
                onClick = onMagnifier,
                onLongClick = { onLongPress(label) },
            )
        }

        HomeButton.ALL_APPS -> {
            val label = stringResource(R.string.apps)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_apps_grid,
                color = ColorAllApps,
                modifier = modifier,
                onClick = onAllApps,
                onLongClick = { onLongPress(label) },
            )
        }

        HomeButton.SPEED_DIAL -> {
            val label = stringResource(R.string.home_button_people)
            val ttsText = stringResource(R.string.home_tts_people)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_contact_placeholder,
                color = ColorSpeedDial,
                modifier = modifier,
                onClick = onSpeedDial,
                onLongClick = { onLongPress(ttsText) },
            )
        }

        HomeButton.MEDICATIONS -> {
            val label = stringResource(R.string.nav_meds)
            val ttsText = stringResource(R.string.home_tts_meds)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_pill,
                color = ColorMeds,
                modifier = modifier,
                onClick = onMedications,
                onLongClick = { onLongPress(ttsText) },
            )
        }

        HomeButton.FLASHLIGHT -> {
            val lightOnLabel = stringResource(R.string.home_button_light_on)
            val flashLabel = stringResource(R.string.flash_light)
            HomeActionButton(
                label = if (isFlashlightOn) lightOnLabel else flashLabel,
                iconRes = R.drawable.ic_flashlight,
                color = if (isFlashlightOn) ColorFlashlight else ColorFlashlight.copy(alpha = 0.6f),
                modifier = modifier,
                onClick = onFlashlight,
                onLongClick = { onLongPress(if (isFlashlightOn) flashlightOnText else flashlightOffText) },
            )
        }

        HomeButton.WEB -> {
            val label = stringResource(R.string.web)
            val ttsText = stringResource(R.string.home_tts_web)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_web,
                color = ColorWeb,
                modifier = modifier,
                onClick = onWeb,
                onLongClick = { onLongPress(ttsText) },
            )
        }

        HomeButton.MAPS -> {
            val label = stringResource(R.string.maps)
            val ttsText = stringResource(R.string.home_tts_maps)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_map,
                color = ColorMaps,
                modifier = modifier,
                onClick = onMaps,
                onLongClick = { onLongPress(ttsText) },
            )
        }

        HomeButton.EMAIL -> {
            val label = stringResource(R.string.email)
            val ttsText = stringResource(R.string.home_tts_email)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_email,
                color = ColorEmail,
                modifier = modifier,
                onClick = onEmail,
                onLongClick = { onLongPress(ttsText) },
            )
        }

        HomeButton.PHOTOS -> {
            val label = stringResource(R.string.photos)
            val ttsText = stringResource(R.string.home_tts_photos)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_photos,
                color = ColorPhotos,
                modifier = modifier,
                onClick = onPhotos,
                onLongClick = { onLongPress(ttsText) },
            )
        }

        HomeButton.YOUTUBE -> {
            val label = stringResource(R.string.youtube)
            val ttsText = stringResource(R.string.home_tts_youtube)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_youtube,
                color = ColorYouTube,
                modifier = modifier,
                onClick = onYouTube,
                onLongClick = { onLongPress(ttsText) },
            )
        }

        HomeButton.CALCULATOR -> {
            val label = stringResource(R.string.calculator)
            val ttsText = stringResource(R.string.home_tts_calculator)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_calculator,
                color = ColorCalculator,
                modifier = modifier,
                onClick = onCalculator,
                onLongClick = { onLongPress(ttsText) },
            )
        }

        // Defensive coding - shouldn't happen...
        else -> android.util.Log.e(
            "HomeScreen",
            "Unhandled HomeButton: $button — add a branch to SingleHomeButton",
        )
    }
}

// ── Button composable ─────────────────────────────────────────────────────────

// Boosts brightness (HSV value) only, leaving hue/saturation untouched — boosting
// R/G/B channels independently shifts hue for saturated colors once one channel clamps.
private fun boostBrightness(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[2] = (hsv[2] * 1.3f).coerceAtMost(1f)
    return Color(android.graphics.Color.HSVToColor((color.alpha * 255).toInt(), hsv))
}

@Composable
private fun HomeActionButton(
    label: String,
    iconRes: Int,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // BoxWithConstraints exposes maxWidth/maxHeight (after the 8dp padding is
    // subtracted) so the icon can scale with whatever space the grid gives this
    // button rather than using a fixed dp value.
    //
    // Sizing rule: 66% of the button width, but capped at 55% of the button
    // height so the label always has room beneath it. No hard upper cap — larger
    // screens/fewer buttons naturally produce larger icons.
    val effectiveColor = if (LocalHighContrast.current) {
        boostBrightness(color)
    } else {
        color
    }
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(effectiveColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
    ) {
        val iconSize = minOf(maxWidth * 0.66f, maxHeight * 0.55f).coerceAtLeast(24.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize),
            )
            AutoSizeLabel(
                text = label,
                // Full inner width — the 8dp padding on the button already keeps
                // text off the rounded corners, so long labels get every pixel.
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Auto-sizing button label ──────────────────────────────────────────────────

/**
 * Single-line label that renders at the largest size in
 * [[minFontSizeSp], [maxFontSizeSp]] that fits the available width, so long
 * words like "Flashlight" and "Magnifier" stay as large as possible without
 * being clipped. Compose measures the fit directly, so there is no per-frame
 * shrink loop to converge or reset between label changes.
 */
@Composable
private fun AutoSizeLabel(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSizeSp: Float = 20f,
    minFontSizeSp: Float = 10f,
) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minFontSizeSp.sp,
            maxFontSize = maxFontSizeSp.sp,
            stepSize = 1.sp,
        ),
        modifier = modifier,
    )
}

// ── Flashlight effect ─────────────────────────────────────────────────────────

@Composable
private fun FlashlightEffect(enabled: Boolean) {
    val context = LocalContext.current

    DisposableEffect(enabled) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val torchCameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager
                    .getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }

        if (torchCameraId != null) {
            try {
                cameraManager.setTorchMode(torchCameraId, enabled)
            } catch (_: Exception) {
            }
        }

        onDispose {
            // Only turn off the torch if this effect instance turned it ON.
            // If enabled==false here we never set it to true, so there's nothing
            // to undo. Avoiding the redundant setTorchMode(false) call prevents
            // a brief torch-off flicker on devices with hardware rate limiting
            // (e.g. Samsung) when the effect re-runs due to recomposition.
            if (enabled && torchCameraId != null) {
                try {
                    cameraManager.setTorchMode(torchCameraId, false)
                } catch (_: Exception) {
                }
            }
        }
    }
}
