package com.tuckercr.ezlauncher.presentation.fall

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.ezlauncher.R

private val ColorAlertBg = Color(0xFFB71C1C) // deep red background
private val ColorAlertSurface = Color(0xFFD32F2F) // slightly lighter for cards
private val ColorOkButton = Color(0xFF2E7D32) // green — "I'm OK"
private val ColorCallButton = Color(0xFFEF5350) // bright red — "Call Now"

@Composable
fun FallAlertScreen(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: FallAlertViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Navigate away once dismissed (either manually or after call initiated)
    LaunchedEffect(state.dismissed) {
        if (state.dismissed) onDismiss()
    }

    val progress by animateFloatAsState(
        targetValue = state.secondsRemaining / 30f,
        animationSpec = tween(durationMillis = 900),
        label = "countdown",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorAlertBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            // ── Title ────────────────────────────────────────────────────────
            Text(
                text = "⚠️",
                fontSize = 64.sp,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.fall_alert_title),
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
            )

            // ── Countdown ring ────────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(180.dp),
                    strokeWidth = 12.dp,
                    color = Color.White,
                    trackColor = ColorAlertSurface,
                    strokeCap = StrokeCap.Round,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.secondsRemaining}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(R.string.fall_alert_seconds),
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            // ── Contact info ──────────────────────────────────────────────────
            val contact = state.primaryContact
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(ColorAlertSurface)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth(),
            ) {
                if (contact != null) {
                    Text(
                        stringResource(R.string.fall_alert_calling, state.secondsRemaining),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        contact.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        contact.phoneNumber,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                    )
                } else {
                    Text(
                        stringResource(R.string.fall_alert_no_contact),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.fall_alert_set_contact),
                            color = ColorAlertBg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // "I'm OK" — cancel everything
                Button(
                    onClick = { viewModel.dismiss() },
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorOkButton),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        stringResource(R.string.fall_alert_im_ok),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                // "Call Now" — skip countdown
                Button(
                    onClick = { viewModel.callNow() },
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    enabled = contact != null,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorCallButton),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        stringResource(R.string.fall_alert_call_now),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
