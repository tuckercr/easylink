package com.tuckercr.ezlauncher.presentation.clock

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(
    viewModel: ClockViewModel = hiltViewModel(),
) {
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val timer       by viewModel.timerState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Clock & Timer", fontWeight = FontWeight.Bold) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.height(16.dp))

            // ── Live clock ────────────────────────────────────────────────
            Text(
                text = currentTime,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(36.dp))

            // ── Timer section ─────────────────────────────────────────────
            Text(
                text = "TIMER",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            // Preset duration chips
            PresetRow(
                presets = listOf(
                    "5 min"  to 5 * 60,
                    "10 min" to 10 * 60,
                    "20 min" to 20 * 60,
                    "30 min" to 30 * 60,
                ),
                onSelect = { viewModel.setTimer(it) },
            )

            Spacer(Modifier.height(8.dp))

            PresetRow(
                presets = listOf(
                    "1 hr"   to 60 * 60,
                    "2 hr"   to 120 * 60,
                    "5 min"  to 5 * 60,   // kept for balance — alternative row
                ),
                onSelect = { viewModel.setTimer(it) },
            )

            Spacer(Modifier.height(32.dp))

            // Timer ring + time display
            TimerRing(timer = timer)

            Spacer(Modifier.height(32.dp))

            // Control buttons
            if (timer.isSet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(
                        onClick = { viewModel.startPause() },
                        modifier = Modifier.weight(1f).height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timer.isRunning) Color(0xFFE65100)
                                             else Color(0xFF2E7D32),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = when {
                                timer.isFinished -> "Done"
                                timer.isRunning  -> "Pause"
                                else             -> "Start"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Reset", fontSize = 22.sp, color = Color.White)
                    }
                }
            }

            if (timer.isFinished) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B5E20))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "⏰  Timer finished!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5D6A7),
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetRow(
    presets: List<Pair<String, Int>>,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { (label, seconds) ->
            OutlinedButton(
                onClick = { onSelect(seconds) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(label, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun TimerRing(timer: TimerState) {
    val animatedProgress by animateFloatAsState(
        targetValue = timer.progress,
        animationSpec = tween(durationMillis = 800),
        label = "timerProgress",
    )
    val ringColor by animateColorAsState(
        targetValue = when {
            timer.isFinished          -> Color(0xFF4CAF50)
            timer.remainingSeconds <= 10 && timer.isSet -> Color(0xFFEF5350)
            timer.isRunning           -> Color(0xFF42A5F5)
            else                      -> MaterialTheme.colorScheme.primary
        },
        label = "timerColor",
    )

    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(200.dp),
            strokeWidth = 10.dp,
            color = ringColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (timer.isSet) timer.displayTime else "--:--",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = if (timer.isFinished) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.onSurface,
                letterSpacing = 2.sp,
            )
            if (timer.isRunning) {
                Text(
                    text = "running",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
