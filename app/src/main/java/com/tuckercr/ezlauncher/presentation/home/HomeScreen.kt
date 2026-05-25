package com.tuckercr.ezlauncher.presentation.home

import android.content.Intent
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.presentation.tts.TtsViewModel
import com.tuckercr.ezlauncher.ui.theme.ColorAllApps
import com.tuckercr.ezlauncher.ui.theme.ColorCamera
import com.tuckercr.ezlauncher.ui.theme.ColorFlashlight
import com.tuckercr.ezlauncher.ui.theme.ColorMagnifier
import com.tuckercr.ezlauncher.ui.theme.ColorPhone
import com.tuckercr.ezlauncher.ui.theme.ColorSos
import com.tuckercr.ezlauncher.ui.theme.ColorText

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    ttsViewModel: TtsViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner
    ),
    onNavigateToApps: () -> Unit,
    onNavigateToMagnifier: () -> Unit,
    onNavigateToSos: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Flashlight torch control — only binds camera when torch is on
    val flashlightOn = (state as? HomeUiState.Success)?.isFlashlightOn ?: false
    FlashlightEffect(enabled = flashlightOn)

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
                // ── Header: battery + time ──────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(
                                when {
                                    s.batteryState.isCharging         -> R.drawable.ic_battery_charging
                                    s.batteryState.levelPercent <= 20 -> R.drawable.ic_battery_low
                                    else                              -> R.drawable.ic_battery_full
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White,
                        )
                        Text(
                            text = if (s.batteryState.levelPercent >= 0)
                                stringResource(R.string.battery_level, s.batteryState.levelPercent)
                            else
                                stringResource(R.string.battery_unknown),
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
                }

                Spacer(Modifier.weight(1f))

                // ── 2 rows of 3 buttons ─────────────────────────────────────
                val view = LocalView.current
                val launchIntent: (Intent) -> Unit = { intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                }
                // Resolve strings in composable scope (can't call stringResource inside lambdas)
                val flashlightOnText = stringResource(R.string.flashlight_on)
                val flashlightOffText = stringResource(R.string.flashlight_off)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HomeButton(
                            label = "Phone",
                            iconRes = R.drawable.ic_call,
                            color = ColorPhone,
                            modifier = Modifier.weight(1f),
                            onClick = { launchIntent(Intent(Intent.ACTION_DIAL)) },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                ttsViewModel.speak("Phone")
                            },
                        )
                        HomeButton(
                            label = "Text",
                            iconRes = R.drawable.ic_sms,
                            color = ColorText,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                launchIntent(Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                                })
                            },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                ttsViewModel.speak("Text messages")
                            },
                        )
                        HomeButton(
                            label = "Camera",
                            iconRes = R.drawable.ic_home, // placeholder — replace with camera icon
                            color = ColorCamera,
                            modifier = Modifier.weight(1f),
                            onClick = { launchIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                ttsViewModel.speak("Camera")
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HomeButton(
                            label = "Magnifier",
                            iconRes = R.drawable.ic_home, // placeholder — replace with magnifier icon
                            color = ColorMagnifier,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMagnifier,
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                ttsViewModel.speak("Magnifier")
                            },
                        )
                        HomeButton(
                            label = "All Apps",
                            iconRes = R.drawable.ic_home, // placeholder — replace with apps icon
                            color = ColorAllApps,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToApps,
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                ttsViewModel.speak("All apps")
                            },
                        )
                        HomeButton(
                            label = if (s.isFlashlightOn) "Light On" else "Flashlight",
                            iconRes = R.drawable.ic_home, // placeholder — replace with flashlight icon
                            color = if (s.isFlashlightOn) ColorFlashlight else ColorFlashlight.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.toggleFlashlight() },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                ttsViewModel.speak(
                                    if (s.isFlashlightOn) flashlightOnText else flashlightOffText
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── SOS (full width) ────────────────────────────────────────
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
}

@Composable
private fun HomeButton(
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

/** Binds a camera (no preview surface) just to control the torch. Releases on dispose. */
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
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
                )
                cam?.cameraControl?.enableTorch(true)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
        }
    }
}
