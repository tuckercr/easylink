package com.fangjet.launcher.presentation.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.R
import com.fangjet.launcher.domain.model.HomeButton
import com.fangjet.launcher.presentation.tts.TtsViewModel
import com.fangjet.launcher.presentation.voice.VoiceCommandViewModel
import com.fangjet.launcher.presentation.voice.VoiceEffect
import com.fangjet.launcher.presentation.voice.VoiceOverlay
import com.fangjet.launcher.presentation.voice.VoiceUiState
import com.fangjet.launcher.ui.theme.ColorCalculator
import com.fangjet.launcher.ui.theme.ColorCamera
import com.fangjet.launcher.ui.theme.ColorEmail
import com.fangjet.launcher.ui.theme.ColorFacebook
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
    var showLocationDeniedDialog by remember { mutableStateOf(false) }
    val locationAskedBefore by viewModel.locationPermissionRequested.collectAsStateWithLifecycle()
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.refreshWeather() }

    // RECORD_AUDIO permission launcher — called when user taps the mic button
    val context = LocalContext.current

    // Auto-recover the weather card when its blocker is cleared OUTSIDE the
    // card's own tap flow — permission granted from app settings or another
    // screen, or location services switched back on in quick settings. Runs
    // when Home resumes and whenever the weather state changes while shown;
    // without it the card sits on "Tap to enable weather" until tapped.
    val blockedWeather = ((state as? HomeUiState.Success)?.weather)
        ?.takeIf { it is WeatherInfo.PermissionNeeded || it is WeatherInfo.LocationDisabled }
    LifecycleResumeEffect(blockedWeather) {
        val cleared = when (blockedWeather) {
            is WeatherInfo.PermissionNeeded ->
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

            is WeatherInfo.LocationDisabled ->
                context
                    .getSystemService(LocationManager::class.java)
                    ?.let { LocationManagerCompat.isLocationEnabled(it) } == true

            else -> false
        }
        if (cleared) viewModel.refreshWeather()
        onPauseOrDispose { }
    }
    // After a hard denial the system stops showing its permission dialog,
    // which used to leave the voice bar a dead button. When a request comes
    // back denied with no rationale available ("don't ask again"), explain
    // and offer Settings — or hiding the bar for users who never wanted voice.
    var showMicDeniedDialog by remember { mutableStateOf(false) }
    val activity = context as? Activity
    val micRequestedBefore by viewModel.micPermissionRequested.collectAsStateWithLifecycle()
    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        when {
            granted -> voiceViewModel.startListening()
            activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO,
                ) -> showMicDeniedDialog = true
        }
    }

    val proceedToMic: () -> Unit = {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        val rationaleAvailable = activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.RECORD_AUDIO,
            )
        when {
            hasAudio -> voiceViewModel.startListening()
            // Permanently denied ("don't ask again"): no rationale AND we have
            // asked before. Launching now would be silently swallowed by the
            // system — without even a result callback — so triage here.
            micRequestedBefore && !rationaleAvailable -> showMicDeniedDialog = true
            else -> {
                viewModel.markMicPermissionRequested()
                audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // First-ever tap shows a one-time explainer with usage examples before
    // falling through to the normal permission/listening flow. Marked shown
    // the moment the dialog appears (not on confirm) so it truly never shows
    // twice, even if the user backs out instead of proceeding.
    var showVoiceIntroDialog by remember { mutableStateOf(false) }
    val voiceIntroShown by viewModel.voiceIntroShown.collectAsStateWithLifecycle()
    val onMicTapped: () -> Unit = {
        if (voiceIntroShown) {
            proceedToMic()
        } else {
            viewModel.markVoiceIntroShown()
            showVoiceIntroDialog = true
        }
    }

    val openAppSettings: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${context.packageName}".toUri(),
                ),
            )
        }
    }

    if (showVoiceIntroDialog) {
        VoiceIntroDialog(
            onStart = {
                showVoiceIntroDialog = false
                proceedToMic()
            },
            onDismiss = { showVoiceIntroDialog = false },
        )
    }

    if (showMicDeniedDialog) {
        PermissionDeniedDialog(
            title = stringResource(R.string.voice_mic_denied_title),
            body = stringResource(R.string.voice_mic_denied_body),
            onOpenSettings = {
                showMicDeniedDialog = false
                openAppSettings()
            },
            onDismiss = { showMicDeniedDialog = false },
            secondaryActionLabel = stringResource(R.string.voice_mic_hide_button),
            onSecondaryAction = {
                showMicDeniedDialog = false
                viewModel.hideVoiceButton()
            },
        )
    }

    if (showLocationDeniedDialog) {
        PermissionDeniedDialog(
            title = stringResource(R.string.weather_location_denied_title),
            body = stringResource(R.string.weather_location_denied_body),
            onOpenSettings = {
                showLocationDeniedDialog = false
                openAppSettings()
            },
            onDismiss = { showLocationDeniedDialog = false },
        )
    }

    // ── Display Size neutralization ──────────────────────────────────────────
    // The grid fills the screen by weight, so the system Display Size setting
    // cannot make its buttons bigger — it only grows every fixed-dp margin and
    // chrome element (screen padding, weather card, apps row, voice bar),
    // which physically SQUEEZES the grid and shrinks its icons and labels.
    // The home screen is already maximized for the physical panel, so render
    // it at the device's stable density: Display Size affects every other
    // screen normally, and the separate font-size setting (fontScale) is
    // still honoured here.
    val currentDensity = LocalDensity.current
    val stableDensity = remember { DisplayMetrics.DENSITY_DEVICE_STABLE / 160f }

    // ── Root Box allows overlay on top of screen content ─────────────────────
    CompositionLocalProvider(
        LocalDensity provides Density(stableDensity, currentDensity.fontScale),
    ) {
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
                                    val rationaleAvailable = activity != null &&
                                        ActivityCompat.shouldShowRequestPermissionRationale(
                                            activity,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        )
                                    // Permanently denied: the system swallows
                                    // the request without a callback — the card
                                    // would be a dead button. Offer Settings;
                                    // the resume-recovery effect refreshes
                                    // automatically once granted there.
                                    if (locationAskedBefore && !rationaleAvailable) {
                                        showLocationDeniedDialog = true
                                    } else {
                                        viewModel.markLocationPermissionRequested()
                                        locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
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
                            showVoiceButton = s.voiceButtonEnabled,
                            onVoiceTapped = onMicTapped,
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
                            onFacebook = {
                                context.packageManager
                                    .getLaunchIntentForPackage("com.facebook.katana")
                                    ?.let { launchIntent(it) }
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

                        // ── All Apps handle ──────────────────────────────────
                        // Fixed and deliberately not customizable: every phone
                        // function lives behind it, so there is no scenario
                        // where a user or carer should be able to remove it.
                        // Mirrors the swipe-up gesture, which opens the same
                        // screen.
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onNavigateToApps),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_apps_grid),
                                contentDescription = stringResource(R.string.apps),
                                tint = Color.White.copy(alpha = 0.9f),
                            )
                        }
                    }
                }
            }

            // ── Voice overlay (rendered on top when active) ───────────────────
            if (voiceState !is VoiceUiState.Idle) {
                VoiceOverlay(
                    state = voiceState,
                    onDismiss = { voiceViewModel.dismiss() },
                    onRetry = { voiceViewModel.retry() },
                )
            }
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
                        fontSize = 14.sp,
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

/**
 * One cell in the grid — either a persisted [HomeButton] or the voice command
 * tile, which is driven by its own Remote Config + user-preference flow
 * ([HomeUiState.Success.voiceButtonEnabled]) rather than the generic
 * enable/disable list, but renders as an equal-sized tile alongside the rest.
 */
private sealed interface GridTile {
    data class ButtonTile(
        val button: HomeButton,
    ) : GridTile

    data object VoiceTile : GridTile
}

@Composable
private fun ButtonGrid(
    enabledButtons: List<HomeButton>,
    showVoiceButton: Boolean,
    onVoiceTapped: () -> Unit,
    isFlashlightOn: Boolean,
    onPhone: () -> Unit,
    onText: () -> Unit,
    onCamera: () -> Unit,
    onMagnifier: () -> Unit,
    onSpeedDial: () -> Unit,
    onMedications: () -> Unit,
    onFlashlight: () -> Unit,
    onWeb: () -> Unit,
    onFacebook: () -> Unit,
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
    // Voice sorts last so its position doesn't reshuffle the buttons the user
    // already knows the location of when the feature toggles on/off.
    val tiles: List<GridTile> = buildList {
        addAll(enabledButtons.map { GridTile.ButtonTile(it) })
        if (showVoiceButton) add(GridTile.VoiceTile)
    }
    if (tiles.isEmpty()) return

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
        // SOS button / All Apps handle. Each row then takes an equal share.
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            // Adaptive column count: 3 on phones, up to 5 on wide/landscape
            // tablets, so buttons stay button-sized instead of stretching into
            // panels when the launcher fills a large screen.
            val buttonsPerRow = (maxWidth / 200.dp).toInt().coerceIn(3, 5)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                tiles.chunked(buttonsPerRow).forEach { rowTiles ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowTiles.forEach { tile ->
                            when (tile) {
                                is GridTile.ButtonTile -> SingleHomeButton(
                                    button = tile.button,
                                    isFlashlightOn = isFlashlightOn,
                                    onPhone = onPhone,
                                    onText = onText,
                                    onCamera = onCamera,
                                    onMagnifier = onMagnifier,
                                    onSpeedDial = onSpeedDial,
                                    onMedications = onMedications,
                                    onFlashlight = onFlashlight,
                                    onWeb = onWeb,
                                    onFacebook = onFacebook,
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

                                GridTile.VoiceTile -> {
                                    val label = stringResource(R.string.home_voice_command_button)
                                    HomeActionButton(
                                        label = label,
                                        iconRes = R.drawable.ic_mic,
                                        color = ColorVoice,
                                        modifier = Modifier.weight(1f),
                                        onClick = onVoiceTapped,
                                        onLongClick = { onLongPress(label) },
                                    )
                                }
                            }
                        }
                        // Fill empty slots so partial rows keep consistent button widths
                        repeat(buttonsPerRow - rowTiles.size) {
                            Spacer(Modifier.weight(1f))
                        }
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
    onSpeedDial: () -> Unit,
    onMedications: () -> Unit,
    onFlashlight: () -> Unit,
    onWeb: () -> Unit,
    onFacebook: () -> Unit,
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

        HomeButton.FACEBOOK -> {
            val label = stringResource(R.string.facebook)
            HomeActionButton(
                label = label,
                iconRes = R.drawable.ic_facebook,
                color = ColorFacebook,
                modifier = modifier,
                onClick = onFacebook,
                onLongClick = { onLongPress(label) },
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

// ── Permanently-denied permission dialog ──────────────────────────────────────

/**
 * Shown when a permission is permanently denied and its feature would
 * otherwise be a dead button: offers the app's Settings page, an optional
 * secondary action (e.g. hiding the voice bar), and Not Now. Elder-facing:
 * large text, tall full-width buttons, no side-by-side actions.
 */
@Composable
private fun PermissionDeniedDialog(
    title: String,
    body: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = body,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(
                        stringResource(R.string.permission_open_settings),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (secondaryActionLabel != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        Text(secondaryActionLabel, fontSize = 18.sp)
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(R.string.voice_mic_not_now),
                        fontSize = 18.sp,
                    )
                }
            }
        },
        confirmButton = {},
    )
}

/**
 * One-time explainer shown the first time the voice tile is tapped: what to
 * expect and a few example phrases, before falling through to the normal
 * permission/listening flow. See [HomePreferencesDataSource.voiceIntroShown].
 */
@Composable
private fun VoiceIntroDialog(
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.voice_intro_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.voice_intro_body),
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(
                        stringResource(R.string.voice_intro_start),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(R.string.voice_mic_not_now),
                        fontSize = 18.sp,
                    )
                }
            }
        },
        confirmButton = {},
    )
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
