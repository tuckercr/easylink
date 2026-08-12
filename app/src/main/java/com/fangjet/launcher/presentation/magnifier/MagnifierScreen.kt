package com.fangjet.launcher.presentation.magnifier

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.R
import com.fangjet.launcher.presentation.common.BigBackButton

@Composable
fun MagnifierScreen(
    onBack: () -> Unit,
    viewModel: MagnifierViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Camera permission gate ────────────────────────────────────────────────
    // Onboarding asks for CAMERA but every step is skippable, so this screen
    // must be able to obtain the permission itself — without it CameraX binds
    // to nothing and the elder just sees a black screen.
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            // Denied with "don't ask again" (or repeated denials): the system
            // dialog will no longer appear, so the only path is app settings.
            val activity = context as? Activity
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.CAMERA,
                )
        }
    }

    // Re-check when returning from system settings so a grant there flips
    // straight into the magnifier.
    LifecycleResumeEffect(Unit) {
        hasCameraPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }

    if (!hasCameraPermission) {
        CameraPermissionContent(
            permanentlyDenied = permanentlyDenied,
            onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri(),
                        ),
                    )
                }
            },
            onBack = onBack,
        )
        return
    }

    // Camera instance — shared between the preview and the controls
    var camera by remember { mutableStateOf<Camera?>(null) }

    // One provider future shared by the AndroidView factory (which binds) and
    // the dispose hook (which unbinds). A second listener calling unbindAll()
    // here raced the factory's bind and detached the fresh preview — the
    // "Surfaces closed" black screen.
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // bindToLifecycle only auto-unbinds when the *activity* dies; leaving this
    // screen must release the camera explicitly.
    DisposableEffect(Unit) {
        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }

    // Apply zoom whenever zoomLevel changes
    LaunchedEffect(state.zoomLevel, camera) {
        camera?.cameraControl?.setLinearZoom(
            (
                (state.zoomLevel - MagnifierUiState.MIN_ZOOM) /
                    (MagnifierUiState.MAX_ZOOM - MagnifierUiState.MIN_ZOOM)
            ).coerceIn(0f, 1f),
        )
    }

    // Apply torch whenever flashlight state changes
    LaunchedEffect(state.isFlashlightOn, camera) {
        camera?.cameraControl?.enableTorch(state.isFlashlightOn)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // Camera preview via AndroidView
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            provider.unbindAll()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                            )
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // High-contrast overlay — a semi-transparent dark overlay that boosts contrast
            // Real high-contrast would need a custom shader; this provides a simpler approximation
            if (state.isHighContrast) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f)),
                )
            }

            // Zoom info chip
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${state.zoomLevel}×",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Control panel at the bottom
            ControlPanel(
                state = state,
                onZoomOut = { viewModel.zoomOut() },
                onZoomIn = { viewModel.zoomIn() },
                onToggleFlashlight = { viewModel.toggleFlashlight() },
                onToggleHighContrast = { viewModel.toggleHighContrast() },
                onReset = { viewModel.resetToDefaults() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // ── Large full-width Back button ──────────────────────────────────────
        BigBackButton(onClick = onBack)
    }
}

// ── Camera permission explainer ───────────────────────────────────────────────

@Composable
private fun CameraPermissionContent(
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // Scrollable so the message and Allow button stay reachable at
                // large accessibility font scales.
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "🔍", fontSize = 64.sp)
            Text(
                text = stringResource(R.string.magnifier_permission_title),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = stringResource(
                    if (permanentlyDenied) {
                        R.string.magnifier_permission_denied_body
                    } else {
                        R.string.magnifier_permission_body
                    },
                ),
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
            Button(
                onClick = if (permanentlyDenied) onOpenSettings else onAllow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            ) {
                Text(
                    text = stringResource(
                        if (permanentlyDenied) {
                            R.string.magnifier_permission_open_settings
                        } else {
                            R.string.magnifier_permission_allow
                        },
                    ),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }
        }
        BigBackButton(onClick = onBack)
    }
}

@Composable
private fun ControlPanel(
    state: MagnifierUiState,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onToggleHighContrast: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Zoom row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onZoomOut,
                enabled = state.zoomLevel > MagnifierUiState.MIN_ZOOM,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.magnifier_zoom_out), fontSize = 18.sp)
            }
            Button(
                onClick = onZoomIn,
                enabled = state.zoomLevel < MagnifierUiState.MAX_ZOOM,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.magnifier_zoom_in), fontSize = 18.sp)
            }
        }

        // Flashlight + High Contrast row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onToggleFlashlight,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isFlashlightOn) {
                        Color(0xFFDAB129)
                    } else {
                        Color(0xFF444444)
                    },
                    // The theme's default button text is dark navy — invisible
                    // on these dark containers.
                    contentColor = if (state.isFlashlightOn) Color.Black else Color.White,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (state.isFlashlightOn) {
                        stringResource(R.string.magnifier_light_on)
                    } else {
                        stringResource(R.string.magnifier_light_off)
                    },
                    fontSize = 18.sp,
                )
            }
            Button(
                onClick = onToggleHighContrast,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isHighContrast) {
                        Color(0xFF555599)
                    } else {
                        Color(0xFF444444)
                    },
                    contentColor = Color.White,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (state.isHighContrast) {
                        stringResource(R.string.magnifier_hi_con_on)
                    } else {
                        stringResource(R.string.magnifier_hi_con_off)
                    },
                    fontSize = 18.sp,
                )
            }
        }

        // Reset
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF444444),
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.clock_timer_reset), fontSize = 18.sp)
        }
    }
}
