package com.fangjet.launcher.presentation.magnifier

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnifierScreen(
    onBack: () -> Unit,
    viewModel: MagnifierViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera instance — shared between the preview and the controls
    var camera by remember { mutableStateOf<Camera?>(null) }

    // Bind CameraX once; reuse for zoom + torch
    DisposableEffect(Unit) {
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            provider = future.get()
            provider?.unbindAll()
            // camera is bound in AndroidView's update callback
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.magnifier), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_home),
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Camera preview via AndroidView
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val pvFuture = ProcessCameraProvider.getInstance(ctx)
                        pvFuture.addListener({
                            val provider = pvFuture.get()
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
                Text("− Zoom Out", fontSize = 16.sp)
            }
            Button(
                onClick = onZoomIn,
                enabled = state.zoomLevel < MagnifierUiState.MAX_ZOOM,
                modifier = Modifier.weight(1f),
            ) {
                Text("+ Zoom In", fontSize = 16.sp)
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
                        Color(
                            0xFF444444,
                        )
                    },
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (state.isFlashlightOn) "Light: On" else "Light: Off",
                    fontSize = 16.sp,
                )
            }
            Button(
                onClick = onToggleHighContrast,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isHighContrast) {
                        Color(0xFF555599)
                    } else {
                        Color(
                            0xFF444444,
                        )
                    },
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (state.isHighContrast) "Hi-Con: On" else "Hi-Con: Off",
                    fontSize = 16.sp,
                )
            }
        }

        // Reset
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.clock_timer_reset), fontSize = 16.sp)
        }
    }
}
