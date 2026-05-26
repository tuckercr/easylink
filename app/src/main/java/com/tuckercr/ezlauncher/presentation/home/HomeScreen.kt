package com.tuckercr.ezlauncher.presentation.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.tuckercr.ezlauncher.ui.theme.ColorCamera
import com.tuckercr.ezlauncher.ui.theme.ColorFlashlight
import com.tuckercr.ezlauncher.ui.theme.ColorMagnifier
import com.tuckercr.ezlauncher.ui.theme.ColorPhone
import com.tuckercr.ezlauncher.ui.theme.ColorSos
import com.tuckercr.ezlauncher.ui.theme.ColorText

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
    onNavigateToCustomize: () -> Unit,
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
                    // ── Header: battery + time + customize gear ─────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(
                                    when {
                                        s.batteryState.isCharging -> R.drawable.ic_battery_charging
                                        s.batteryState.levelPercent <= 20 -> R.drawable.ic_battery_low
                                        else -> R.drawable.ic_battery_full
                                    },
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White,
                            )
                            Text(
                                text = if (s.batteryState.levelPercent >= 0) {
                                    stringResource(
                                        R.string.battery_level,
                                        s.batteryState.levelPercent,
                                    )
                                } else {
                                    stringResource(R.string.battery_unknown)
                                },
                                color = Color.White,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }

                        Text(
                            text = s.currentTime,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        // Gear icon → customise screen
                        IconButton(onClick = onNavigateToCustomize) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = "Customise buttons",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

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
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
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
                                "Say a Command",
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
                Text("Getting weather…", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
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
                        Text(
                            text = weather.city?.let { "$it · ${weather.description}" }
                                ?: weather.description,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
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
                    "Tap to enable weather",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
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
                    "Weather unavailable — tap to retry",
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
    onLongPress: (label: String) -> Unit,
    flashlightOnText: String,
    flashlightOffText: String,
) {
    if (enabledButtons.isEmpty()) return

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
    onLongPress: (label: String) -> Unit,
    flashlightOnText: String,
    flashlightOffText: String,
    modifier: Modifier,
) {
    when (button) {
        HomeButton.PHONE -> HomeActionButton(
            label = "Phone",
            iconRes = R.drawable.ic_call,
            color = ColorPhone,
            modifier = modifier,
            onClick = onPhone,
            onLongClick = { onLongPress("Phone") },
        )

        HomeButton.TEXT -> HomeActionButton(
            label = "Text",
            iconRes = R.drawable.ic_sms,
            color = ColorText,
            modifier = modifier,
            onClick = onText,
            onLongClick = { onLongPress("Text messages") },
        )

        HomeButton.CAMERA -> HomeActionButton(
            label = "Camera",
            iconRes = R.drawable.ic_camera,
            color = ColorCamera,
            modifier = modifier,
            onClick = onCamera,
            onLongClick = { onLongPress("Camera") },
        )

        HomeButton.MAGNIFIER -> HomeActionButton(
            label = "Magnifier",
            iconRes = R.drawable.ic_magnifier,
            color = ColorMagnifier,
            modifier = modifier,
            onClick = onMagnifier,
            onLongClick = { onLongPress("Magnifier") },
        )

        HomeButton.ALL_APPS -> HomeActionButton(
            label = "All Apps",
            iconRes = R.drawable.ic_home,
            color = ColorAllApps,
            modifier = modifier,
            onClick = onAllApps,
            onLongClick = { onLongPress("All apps") },
        )

        HomeButton.FLASHLIGHT -> HomeActionButton(
            label = if (isFlashlightOn) "Light On" else "Flashlight",
            iconRes = R.drawable.ic_flashlight,
            color = if (isFlashlightOn) ColorFlashlight else ColorFlashlight.copy(alpha = 0.6f),
            modifier = modifier,
            onClick = onFlashlight,
            onLongClick = { onLongPress(if (isFlashlightOn) flashlightOnText else flashlightOffText) },
        )
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
    if (!enabled) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            provider = future.get()
            try {
                val preview = Preview.Builder().build()
                provider?.unbindAll()
                val cam = provider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                )
                cam?.cameraControl?.enableTorch(true)
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose { provider?.unbindAll() }
    }
}
