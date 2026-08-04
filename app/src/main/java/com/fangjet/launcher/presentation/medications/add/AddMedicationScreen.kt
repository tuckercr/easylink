package com.fangjet.launcher.presentation.medications.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.R
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMedicationScreen(
    onBack: () -> Unit,
    viewModel: AddMedicationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTimePicker by remember { mutableStateOf(false) }

    // Navigate back on success
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onBack()
    }

    // Show save errors
    LaunchedEffect(state.saveError) {
        state.saveError?.let { snackbarHostState.showSnackbar(it) }
    }

    // Compose-native time picker dialog — inherits the dark Material3 theme
    if (showTimePicker) {
        val now = LocalTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = now.hour,
            initialMinute = now.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onReminderTimeAdded(
                        LocalTime.of(timePickerState.hour, timePickerState.minute),
                    )
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.ok), fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel), fontSize = 16.sp)
                }
            },
            title = { Text(stringResource(R.string.add_medication_time_picker_title)) },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) {
                            stringResource(R.string.add_medication_title_edit)
                        } else {
                            stringResource(
                                R.string.add_medication_title_add,
                            )
                        },
                        fontWeight = FontWeight.Bold,
                    )
                },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Name ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onNameChanged(it) },
                label = { Text(stringResource(R.string.add_medication_name_label)) },
                placeholder = { Text(stringResource(R.string.add_medication_name_placeholder)) },
                isError = state.nameError != null,
                supportingText = state.nameError?.let {
                    {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Dosage ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.dosage,
                onValueChange = { viewModel.onDosageChanged(it) },
                label = { Text(stringResource(R.string.add_medication_dosage_label)) },
                placeholder = { Text(stringResource(R.string.add_medication_dosage_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Notes ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onNotesChanged(it) },
                label = { Text(stringResource(R.string.add_medication_notes_label)) },
                placeholder = { Text(stringResource(R.string.add_medication_notes_placeholder)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Reminder times ────────────────────────────────────────────────
            SectionLabel(
                text = stringResource(R.string.add_medication_reminder_times),
                error = state.reminderTimesError,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.reminderTimes.forEach { time ->
                    TimeChip(
                        time = time,
                        onRemove = { viewModel.onReminderTimeRemoved(time) },
                    )
                }
                TextButton(onClick = { showTimePicker = true }) {
                    Text(stringResource(R.string.add_medication_add_time), fontSize = 16.sp)
                }
            }

            // ── Active days ───────────────────────────────────────────────────
            SectionLabel(
                text = stringResource(R.string.add_medication_active_days),
                error = state.activeDaysError,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in state.activeDays,
                        onClick = { viewModel.onDayToggled(day) },
                        label = {
                            Text(
                                text = day.getDisplayName(
                                    TextStyle.SHORT,
                                    LocalLocale.current.platformLocale,
                                ),
                                fontSize = 14.sp,
                            )
                        },
                    )
                }
            }

            // ── Active toggle ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.add_medication_active),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = state.isActive,
                    onCheckedChange = { viewModel.onActiveToggled(it) },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick = { viewModel.onSaveTapped() },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        if (state.isEditing) {
                            stringResource(R.string.add_medication_save_changes)
                        } else {
                            stringResource(
                                R.string.add_medication_save,
                            )
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    error: String? = null,
) {
    Column {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (error != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (error != null) {
            Text(
                text = error,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
private fun TimeChip(
    time: LocalTime,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = time.format(timeFormatter),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "×",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
