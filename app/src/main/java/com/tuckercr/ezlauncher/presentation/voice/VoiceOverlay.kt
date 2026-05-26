package com.tuckercr.ezlauncher.presentation.voice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuckercr.ezlauncher.R

private val OverlayBg = Color(0xCC000000) // 80% black
private val CardBg = Color(0xFF1E1E2E) // dark navy
private val AccentBlue = Color(0xFF6C8EFF)
private val SuccessGreen = Color(0xFF4CAF50)
private val ErrorRed = Color(0xFFEF5350)

/**
 * Full-screen semi-transparent overlay shown while voice recognition is active.
 *
 * Renders different content for each [VoiceUiState]:
 *  - [VoiceUiState.Listening]     → animated waveform + pulsing mic
 *  - [VoiceUiState.Heard]         → recognised text + spinning indicator
 *  - [VoiceUiState.Success]       → green checkmark + action label
 *  - [VoiceUiState.NotUnderstood] → question mark + hint examples + buttons
 *  - [VoiceUiState.Error]         → warning icon + message + buttons
 *
 * [VoiceUiState.Idle] is never passed here — the caller hides the overlay.
 */
@Composable
fun VoiceOverlay(
    state: VoiceUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(OverlayBg),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.92f))
                    .togetherWith(fadeOut(tween(180)))
            },
            label = "voice_state",
        ) { s ->
            when (s) {
                is VoiceUiState.Listening -> ListeningCard(onDismiss)
                is VoiceUiState.Heard -> HeardCard(s.text)
                is VoiceUiState.Success -> SuccessCard(s.actionLabel)
                is VoiceUiState.NotUnderstood -> NotUnderstoodCard(s.text, onRetry, onDismiss)
                is VoiceUiState.Error -> ErrorCard(s.message, onRetry, onDismiss)
                is VoiceUiState.Idle -> { // overlay is hidden before this renders
                }
            }
        }
    }
}

// ── Listening card ─────────────────────────────────────────────────────────────

@Composable
private fun ListeningCard(onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_listening")

    // Pulsing ring behind the mic
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // Five waveform bars with different speeds → natural, unsynced look
    val bar1 by waveBar(infiniteTransition, 0.25f, 1.0f, 340)
    val bar2 by waveBar(infiniteTransition, 0.8f, 0.3f, 500)
    val bar3 by waveBar(infiniteTransition, 0.5f, 1.0f, 420)
    val bar4 by waveBar(infiniteTransition, 0.15f, 0.85f, 380)
    val bar5 by waveBar(infiniteTransition, 0.7f, 0.2f, 470)

    OverlayCard {
        // Pulsing mic circle
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.25f)),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AccentBlue),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = "Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Listening…",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(20.dp))

        // Animated waveform bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(40.dp),
        ) {
            WaveBar(fraction = bar1, color = AccentBlue)
            WaveBar(fraction = bar2, color = AccentBlue.copy(alpha = 0.8f))
            WaveBar(fraction = bar3, color = AccentBlue)
            WaveBar(fraction = bar4, color = AccentBlue.copy(alpha = 0.8f))
            WaveBar(fraction = bar5, color = AccentBlue)
        }

        Spacer(Modifier.height(28.dp))

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        ) {
            Text("Cancel", fontSize = 16.sp)
        }
    }
}

@Composable
private fun waveBar(
    transition: androidx.compose.animation.core.InfiniteTransition,
    from: Float,
    to: Float,
    durationMs: Int,
) = transition.animateFloat(
    initialValue = from,
    targetValue = to,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMs, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
    ),
    label = "bar_$durationMs",
)

@Composable
private fun WaveBar(
    fraction: Float,
    color: Color,
    width: Dp = 8.dp,
    maxHeight: Dp = 40.dp,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(maxHeight * fraction.coerceIn(0.15f, 1f))
            .clip(RoundedCornerShape(4.dp))
            .background(color),
    )
}

// ── Heard card (transient — shown briefly while VM processes) ──────────────────

@Composable
private fun HeardCard(text: String) {
    OverlayCard {
        Text("🎤", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "\"$text\"",
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Processing…",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
        )
    }
}

// ── Success card ───────────────────────────────────────────────────────────────

@Composable
private fun SuccessCard(actionLabel: String) {
    OverlayCard {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SuccessGreen),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "Done",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = actionLabel,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Not understood card ────────────────────────────────────────────────────────

private val VOICE_HINTS = listOf(
    "\"Call John\"",
    "\"Open Camera\"",
    "\"Text Mary\"",
    "\"Flashlight on\"",
    "\"Open YouTube\"",
    "\"All apps\"",
)

@Composable
private fun NotUnderstoodCard(
    text: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    OverlayCard {
        Text("🤔", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "I didn't understand that",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "\"$text\"",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Try saying:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            VOICE_HINTS.forEach { hint ->
                Text(
                    text = "• $hint",
                    color = AccentBlue,
                    fontSize = 15.sp,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        ActionButtons(onRetry = onRetry, onCancel = onCancel)
    }
}

// ── Error card ─────────────────────────────────────────────────────────────────

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    OverlayCard {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ErrorRed),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        ActionButtons(onRetry = onRetry, onCancel = onCancel)
    }
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
private fun OverlayCard(content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(24.dp))
            .background(CardBg)
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        content()
    }
}

@Composable
private fun ActionButtons(
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        ) {
            Text("Cancel", fontSize = 16.sp)
        }
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Try again", fontSize = 16.sp)
        }
    }
}
