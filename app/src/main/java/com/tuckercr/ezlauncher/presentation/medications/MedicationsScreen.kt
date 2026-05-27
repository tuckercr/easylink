package com.tuckercr.ezlauncher.presentation.medications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.domain.model.Medication
import com.tuckercr.ezlauncher.domain.model.MedicationColor
import com.tuckercr.ezlauncher.domain.model.TodayReminder
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    onNavigateToAddMedication: () -> Unit,
    onNavigateToEditMedication: (Long) -> Unit,
    viewModel: MedicationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale.coerceAtMost(1.3f),
        ),
    ) {
        var pendingDelete by remember { mutableStateOf<Medication?>(null) }
        pendingDelete?.let { med ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.medications_delete_title)) },
                text = { Text(stringResource(R.string.medications_delete_body, med.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteMedication(med)
                        pendingDelete = null
                    }) { Text(stringResource(R.string.medications_delete_confirm), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.medications_title), fontWeight = FontWeight.Bold) },
                    actions = {
                        Button(
                            onClick = onNavigateToAddMedication,
                            modifier = Modifier.padding(end = 8.dp),
                        ) { Text(stringResource(R.string.add)) }
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
                when (val s = state) {
                    is MedicationsUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    is MedicationsUiState.Error -> {
                        Text(
                            text = s.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }

                    is MedicationsUiState.Success -> {
                        if (s.reminders.isEmpty()) {
                            EmptyMedications(onAdd = onNavigateToAddMedication)
                        } else {
                            Column {
                                if (s.allDone) AllDoneBanner()
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    ),
                                ) {
                                    items(
                                        s.reminders,
                                        key = { "${it.medication.id}_${it.scheduledAt}" },
                                    ) { reminder ->
                                        ReminderCard(
                                            reminder = reminder,
                                            onEdit = { onNavigateToEditMedication(reminder.medication.id) },
                                            onDelete = { pendingDelete = reminder.medication },
                                            onTaken = { viewModel.markTaken(reminder) },
                                            onSnooze = { viewModel.snooze(reminder) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } // end CompositionLocalProvider
}

// ── Banner ────────────────────────────────────────────────────────────────────

@Composable
private fun AllDoneBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B5E20))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Color(0xFFA5D6A7),
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.medications_all_done),
                color = Color(0xFFA5D6A7),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Card ──────────────────────────────────────────────────────────────────────

@Composable
private fun ReminderCard(
    reminder: TodayReminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
) {
    val cardColor = medicationCardColor(reminder.medication.color)
    val statusColor = statusBackgroundColor(reminder.status)
    val statusText = reminder.status.name
        .lowercase()
        .replaceFirstChar { it.uppercase() }
    val statusTextColor = statusTextColor(reminder.status)

    // Statuses where the user can still act
    val isActionable = reminder.status in setOf(
        TodayReminder.Status.UPCOMING,
        TodayReminder.Status.OVERDUE,
        TodayReminder.Status.SNOOZED,
    )
    val isTaken = reminder.status == TodayReminder.Status.TAKEN

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor),
    ) {
        // ── Top row: info + status chip + delete ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pill),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.medication.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                if (reminder.medication.dosage.isNotBlank()) {
                    Text(
                        text = reminder.medication.dosage,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                Text(
                    text = reminder.scheduledAt.format(DateTimeFormatter.ofPattern("h:mm a")),
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            // Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = statusText,
                    color = statusTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            // Delete
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(onClick = onDelete),
            ) {
                Text("×", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ── Action buttons (only when actionable) ─────────────────────────
        if (isActionable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Taken — primary action, full-weight
                Button(
                    onClick = onTaken,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                    Text(
                        text = stringResource(R.string.medications_taken),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                // Snooze — secondary
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_snooze),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                    Text(
                        text = stringResource(R.string.medications_snooze),
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            }
        }

        // ── Taken confirmation strip ───────────────────────────────────────
        if (isTaken) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B5E20))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color(0xFFA5D6A7),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.medications_taken_well_done),
                    color = Color(0xFFA5D6A7),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyMedications(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pill),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.medications_empty_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.medications_empty_body),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd) { Text(stringResource(R.string.medications_add_medication), fontSize = 18.sp) }
    }
}

// ── Colour helpers ────────────────────────────────────────────────────────────

private fun medicationCardColor(color: MedicationColor): Color =
    when (color) {
        MedicationColor.BLUE -> Color(0xFF1565C0)
        MedicationColor.GREEN -> Color(0xFF2E7D32)
        MedicationColor.AMBER -> Color(0xFFE65100)
        MedicationColor.CORAL -> Color(0xFFC62828)
        MedicationColor.PURPLE -> Color(0xFF4527A0)
    }

private fun statusBackgroundColor(status: TodayReminder.Status): Color =
    when (status) {
        TodayReminder.Status.UPCOMING -> Color(0xFF0D47A1)
        TodayReminder.Status.TAKEN -> Color(0xFF1B5E20)
        TodayReminder.Status.SNOOZED -> Color(0xFFE65100)
        TodayReminder.Status.MISSED -> Color(0xFFBF360C)
        TodayReminder.Status.OVERDUE -> Color(0xFFB71C1C)
    }

private fun statusTextColor(status: TodayReminder.Status): Color =
    when (status) {
        TodayReminder.Status.UPCOMING -> Color(0xFF90CAF9)
        TodayReminder.Status.TAKEN -> Color(0xFFA5D6A7)
        TodayReminder.Status.SNOOZED -> Color(0xFFFFCC80)
        TodayReminder.Status.MISSED -> Color(0xFFFFAB91)
        TodayReminder.Status.OVERDUE -> Color(0xFFEF9A9A)
    }
