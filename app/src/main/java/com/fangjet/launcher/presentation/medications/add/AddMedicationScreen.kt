package com.fangjet.launcher.presentation.medications.add

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    /**
     * Fired on successful save. Separate from [onBack]: the save can complete
     * while the activity is still resuming from the notification-permission
     * dialog, and the nav host's tremor guard would drop a not-yet-RESUMED
     * pop — stranding the user on a form whose medication already saved.
     * This callback must be wired to an unguarded pop.
     */
    onSaved: () -> Unit = onBack,
    viewModel: AddMedicationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTimePicker by remember { mutableStateOf(false) }

    // Contextual notification ask: reminders are the whole point of saving a
    // medication, so this is the moment the permission makes sense. Saving
    // proceeds whatever the answer — alarms still fire; only the banner is
    // suppressed when denied.
    val context = LocalContext.current
    val activity = context as? Activity
    val notifAskedBefore by viewModel.notifPermissionRequested.collectAsStateWithLifecycle()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onSaveTapped() }
    val saveWithNotificationAsk: () -> Unit = {
        // Validate first: an invalid form must show its errors here, not fire
        // the permission prompt (whose resume-chain can navigate away and
        // silently lose the form).
        if (viewModel.validateForSave()) {
            val notGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            // Permanently denied requests are silently swallowed by the system
            // WITHOUT invoking the result callback — launching one here would
            // make Save a dead button and the medication unsaveable. Ask only
            // while a dialog can actually appear; otherwise save without
            // reminders rather than not at all.
            val rationaleAvailable = activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            val canAsk = !notifAskedBefore || rationaleAvailable
            if (notGranted && canAsk) {
                viewModel.markNotifPermissionRequested()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onSaveTapped()
            }
        }
    }

    // Navigate back on success
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onSaved()
    }

    // Show save errors
    LaunchedEffect(state.saveError) {
        state.saveError?.let { snackbarHostState.showSnackbar(it) }
    }

    // Compose-native time picker dialog — inherits the dark Material3 theme
    if (showTimePicker) {
        // Pre-fill the next full hour (6:22 → 7:00): medication times are
        // round-hour schedules, and zeroed minutes save the elder fiddling
        // with the minute dial.
        val nextFullHour = LocalTime.now().plusHours(1).withMinute(0)
        val timePickerState = rememberTimePickerState(
            initialHour = nextFullHour.hour,
            initialMinute = nextFullHour.minute,
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
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                label = { Text(stringResource(R.string.add_medication_name_label)) },
                placeholder = { Text(stringResource(R.string.add_medication_name_placeholder), fontSize = 18.sp) },
                isError = state.nameError != null,
                supportingText = state.nameError?.let {
                    {
                        Text(
                            it,
                            fontSize = 16.sp,
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
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                label = { Text(stringResource(R.string.add_medication_dosage_label)) },
                placeholder = { Text(stringResource(R.string.add_medication_dosage_placeholder), fontSize = 18.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Notes ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onNotesChanged(it) },
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                label = { Text(stringResource(R.string.add_medication_notes_label)) },
                placeholder = { Text(stringResource(R.string.add_medication_notes_placeholder), fontSize = 18.sp) },
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
                    Text(stringResource(R.string.add_medication_add_time), fontSize = 20.sp)
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
                        // Material chips default to 32dp — too small a target
                        // and label for this audience. Selected = the same blue
                        // as the reminder-time chips so "on" is unmistakable.
                        modifier = Modifier.height(48.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        label = {
                            Text(
                                text = day.getDisplayName(
                                    TextStyle.SHORT,
                                    LocalLocale.current.platformLocale,
                                ),
                                fontSize = 18.sp,
                            )
                        },
                    )
                }
            }

            // ── Active toggle (edit only) ─────────────────────────────────────
            // A brand-new medication is always active — offering the toggle on
            // the add form just invites accidentally creating a med that never
            // reminds. Pausing is an edit-time decision.
            if (state.isEditing) {
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
            }

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick = saveWithNotificationAsk,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
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
            fontSize = 18.sp,
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
                fontSize = 16.sp,
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
            fontSize = 18.sp,
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
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
