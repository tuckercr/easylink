package com.tuckercr.ezlauncher.presentation.home

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.domain.model.HomeButton
import com.tuckercr.ezlauncher.domain.model.WeatherInfo
import com.tuckercr.ezlauncher.presentation.tts.TtsViewModel
import com.tuckercr.ezlauncher.presentation.voice.VoiceCommandViewModel
import com.tuckercr.ezlauncher.presentation.voice.VoiceEffect
import com.tuckercr.ezlauncher.presentation.voice.VoiceOverlay
import com.tuckercr.ezlauncher.presentation.voice.VoiceUiState
import com.tuckercr.ezlauncher.ui.theme.ColorAllApps
import com.tuckercr.ezlauncher.ui.theme.ColorCalculator
import com.tuckercr.ezlauncher.ui.theme.ColorCamera
import com.tuckercr.ezlauncher.ui.theme.ColorEmail
import com.tuckercr.ezlauncher.ui.theme.ColorFlashlight
import com.tuckercr.ezlauncher.ui.theme.ColorMagnifier
import com.tuckercr.ezlauncher.ui.theme.ColorMaps
import com.tuckercr.ezlauncher.ui.theme.ColorPhone
import com.tuckercr.ezlauncher.ui.theme.ColorPhotos
import com.tuckercr.ezlauncher.ui.theme.ColorSos
import com.tuckercr.ezlauncher.ui.theme.ColorText
import com.tuckercr.ezlauncher.ui.theme.ColorWeb
import com.tuckercr.ezlauncher.ui.theme.ColorYouTube

private const val BUTTONS_PER_ROW = 3

private val ColorVoice = Color(0xFF5C6BC0) // indigo

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
                    // ── Weather card ────────────────────────────────────────
                    WeatherCard(
                        weather = s.weather,
                        onRequestPermission = {
                            locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        },
                        onRefresh = { viewModel.refreshWeather() },
                        onNavigateToForecast = onNavigateToForecast,
                    )

                    Spacer(Modifier.weight(1f))

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
                        onFlashlight = { viewModel.toggleFlashlight() },
                        onWeb = {
                            // Launch browser app to restore last session, falling back to system default if Chrome is missing.
                            val intent =
                                context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                                    ?: context.packageManager.getLaunchIntentForPackage("com.chrome.beta")
                                    ?: context.packageManager.getLaunchIntentForPackage("com.chrome.dev")
                                    ?: Intent(Intent.ACTION_MAIN).apply {
                                        addCategory(Intent.CATEGORY_APP_BROWSER)
                                    }
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

                    Spacer(Modifier.height(12.dp))

                    // ── Voice command button ─────────────────────────────────
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

                    Spacer(Modifier.height(8.dp))

                    // ── SOS (full width) ─────────────────────────────────────
                    Surface(
                        onClick = onNavigateToSos,
                        color = ColorSos,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "SOS",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 6.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
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
private fun WeatherCard(
    weather: WeatherInfo,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToForecast: () -> Unit,
) {
    val cardModifier = Modifier
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
                        val subtitle = if (weather.usingCachedLocation) {
                            weather.city?.let { "$it · ${weather.description}" }
                                ?: weather.description
                        } else {
                            weather.city?.let { "$it · ${weather.description}" }
                                ?: weather.description
                        }
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                        )
                        if (weather.usingCachedLocation && !weather.usingCachedWeather) {
                            Text(
                                text = stringResource(R.string.weather_saved_location),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                            )
                        }
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            enabledButtons.chunked(BUTTONS_PER_ROW).forEach { rowButtons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                iconRes = R.drawable.ic_home,
                color = ColorAllApps,
                modifier = modifier,
                onClick = onAllApps,
                onLongClick = { onLongPress(label) },
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
    }
}

// ── Button composable ─────────────────────────────────────────────────────────

@Composable
private fun HomeActionButton(
    label: String,
    iconRes: Int,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
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
            if (torchCameraId != null) {
                try {
                    cameraManager.setTorchMode(torchCameraId, false)
                } catch (_: Exception) {
                }
            }
        }
    }
}
