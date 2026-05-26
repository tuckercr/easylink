package com.tuckercr.ezlauncher.presentation.home.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.domain.model.FallSensitivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeHomeScreen(
    onBack: () -> Unit,
    viewModel: CustomizeHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customise", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_home),
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            // ── Section: Home buttons ─────────────────────────────────────────
            item {
                SectionHeader("Home Screen Buttons")
                Text(
                    "Choose which quick-action buttons appear on the Home screen.",
                    fontSize = 14.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            items(state.buttons, key = { it.button.name }) { item ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = item.label,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked         = item.isEnabled,
                        onCheckedChange = { viewModel.toggle(item.button, it) },
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            // ── Section: Fall Detection ───────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader("Fall Detection")
                Text(
                    "Uses the phone's accelerometer to detect a fall and automatically " +
                    "call your emergency contact after 30 seconds.",
                    fontSize = 14.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                FallDetectionCard(
                    enabled     = state.fallDetectionEnabled,
                    sensitivity = state.fallSensitivity,
                    onToggle    = { viewModel.setFallDetectionEnabled(it) },
                    onSensitivity = { viewModel.setFallSensitivity(it) },
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Fall Detection card ───────────────────────────────────────────────────────

@Composable
private fun FallDetectionCard(
    enabled: Boolean,
    sensitivity: FallSensitivity,
    onToggle: (Boolean) -> Unit,
    onSensitivity: (FallSensitivity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        // Enable / disable row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Enable Fall Detection",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                if (enabled) {
                    Text(
                        "🛡️ Active — monitoring in background",
                        fontSize = 13.sp,
                        color    = Color(0xFF81C784),
                    )
                } else {
                    Text(
                        "Tap to turn on",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }

        // Sensitivity — only visible when enabled
        if (enabled) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            Text(
                "Sensitivity",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FallSensitivity.entries.forEach { level ->
                    val selected = level == sensitivity
                    OutlinedButton(
                        onClick  = { onSensitivity(level) },
                        colors   = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                                             else Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            level.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 13.sp,
                            color    = if (selected) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                sensitivity.label,
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title,
        fontSize   = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}
