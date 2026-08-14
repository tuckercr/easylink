package com.fangjet.launcher.presentation.speeddial.add

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fangjet.launcher.R
import com.fangjet.launcher.domain.model.DeviceContact
import com.fangjet.launcher.ui.theme.ColorSpeedDial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpeedDialScreen(
    onBack: () -> Unit,
    /** Fired on successful save — wire to an UNGUARDED pop (see AddMedicationScreen.onSaved). */
    onSaved: () -> Unit = onBack,
    viewModel: AddSpeedDialViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val strAlreadyAdded = stringResource(R.string.add_speed_dial_already_added)
    val strInvalidContact = stringResource(R.string.add_speed_dial_invalid_contact)

    LaunchedEffect(state.saveResult) {
        when (val result = state.saveResult) {
            is AddSpeedDialUiState.SaveResult.Success -> onSaved()
            is AddSpeedDialUiState.SaveResult.AlreadyAdded -> {
                snackbarHostState.showSnackbar(strAlreadyAdded)
                viewModel.onSaveResultConsumed()
            }

            is AddSpeedDialUiState.SaveResult.InvalidContact -> {
                snackbarHostState.showSnackbar(strInvalidContact)
                viewModel.onSaveResultConsumed()
            }

            is AddSpeedDialUiState.SaveResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.onSaveResultConsumed()
            }

            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditMode) R.string.edit_person_title else R.string.add_speed_dial_title,
                        ),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
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
                .padding(innerPadding),
        ) {
            // ── Mode tabs (add mode only — editing always uses the form) ──
            if (!state.isEditMode) {
                TabRow(selectedTabIndex = if (state.isManualEntry) 1 else 0) {
                    Tab(
                        selected = !state.isManualEntry,
                        onClick = { if (state.isManualEntry) viewModel.onToggleManualEntry() },
                        text = {
                            Text(
                                stringResource(R.string.add_speed_dial_tab_search),
                                fontSize = 16.sp,
                            )
                        },
                    )
                    Tab(
                        selected = state.isManualEntry,
                        onClick = { if (!state.isManualEntry) viewModel.onToggleManualEntry() },
                        text = {
                            Text(
                                stringResource(R.string.add_speed_dial_tab_manual),
                                fontSize = 16.sp,
                            )
                        },
                    )
                }
            }

            if (state.isManualEntry || state.isEditMode) {
                ManualEntryPanel(state = state, viewModel = viewModel)
            } else {
                SearchPanel(state = state, viewModel = viewModel)
            }
        }
    }
}

// ── Manual entry / edit form ──────────────────────────────────────────────────

@Composable
private fun ManualEntryPanel(
    state: AddSpeedDialUiState,
    viewModel: AddSpeedDialViewModel,
) {
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onPhotoPicked(uri) }
    val pickPhoto = {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        DeletePersonDialog(
            name = state.manualName,
            onConfirm = {
                confirmDelete = false
                viewModel.onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!state.isEditMode) {
            Text(
                text = stringResource(R.string.add_speed_dial_manual_body),
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Profile photo ─────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PhotoPreview(
                photoUri = state.manualPhotoUri,
                name = state.manualName,
                onClick = pickPhoto,
            )
            OutlinedButton(onClick = pickPhoto) {
                Text(
                    stringResource(
                        if (state.manualPhotoUri != null) {
                            R.string.person_photo_change
                        } else {
                            R.string.person_photo_add
                        },
                    ),
                    fontSize = 17.sp,
                )
            }
        }

        OutlinedTextField(
            value = state.manualName,
            onValueChange = { viewModel.onManualNameChanged(it) },
            label = { Text(stringResource(R.string.field_name), fontSize = 16.sp) },
            placeholder = { Text(stringResource(R.string.add_speed_dial_name_placeholder)) },
            isError = state.manualNameError != null,
            supportingText = state.manualNameError?.let { { Text(it, fontSize = 14.sp) } },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.manualPhone,
            onValueChange = { viewModel.onManualPhoneChanged(it) },
            label = { Text(stringResource(R.string.field_phone_number), fontSize = 16.sp) },
            placeholder = { Text(stringResource(R.string.field_phone_placeholder)) },
            isError = state.manualPhoneError != null,
            supportingText = state.manualPhoneError?.let { { Text(it, fontSize = 14.sp) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { viewModel.onManualSave() },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp),
        ) {
            Text(
                stringResource(
                    if (state.isEditMode) R.string.save else R.string.add_speed_dial_save_button,
                ),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (state.isEditMode) {
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
            ) {
                Text(
                    stringResource(R.string.people_delete_button),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PhotoPreview(
    photoUri: Uri?,
    name: String,
    onClick: () -> Unit,
) {
    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = stringResource(R.string.person_photo_change),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
        )
    } else {
        val initials = name.toInitials()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(ColorSpeedDial)
                .clickable(onClick = onClick),
        ) {
            if (initials.isEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = stringResource(R.string.person_photo_add),
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Text(initials, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Delete confirmation ───────────────────────────────────────────────────────

@Composable
private fun DeletePersonDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.speed_dial_remove_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                stringResource(R.string.speed_dial_remove_body, name),
                fontSize = 19.sp,
                lineHeight = 26.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.speed_dial_remove_confirm),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), fontSize = 19.sp)
            }
        },
    )
}

// ── Contact search panel ──────────────────────────────────────────────────────

@Composable
private fun SearchPanel(
    state: AddSpeedDialUiState,
    viewModel: AddSpeedDialViewModel,
) {
    // Contacts permission gate: onboarding asks for READ_CONTACTS but is
    // skippable, and without it the search silently returns nothing — which
    // reads as "broken", not "locked". Ask here, at the point of use.
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            val activity = context as? Activity
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_CONTACTS,
                )
        }
    }
    LifecycleResumeEffect(Unit) {
        hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }

    if (!hasPermission) {
        ContactsPermissionPanel(
            permanentlyDenied = permanentlyDenied,
            onAllow = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
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
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text(stringResource(R.string.add_speed_dial_search_label)) },
            placeholder = { Text(stringResource(R.string.add_speed_dial_search_placeholder)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.contacts.isEmpty() && state.searchQuery.isNotEmpty() -> {
                    Text(
                        text = stringResource(
                            R.string.add_speed_dial_no_results,
                            state.searchQuery,
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.contacts.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.add_speed_dial_search_hint),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                    )
                }

                else -> {
                    LazyColumn {
                        items(state.contacts, key = { it.contactId }) { contact ->
                            ContactSearchRow(
                                contact = contact,
                                onClick = { viewModel.onContactSelected(contact) },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Contacts permission explainer ─────────────────────────────────────────────

@Composable
private fun ContactsPermissionPanel(
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Scrollable so the message and Allow button stay reachable at
            // large accessibility font scales.
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "📖", fontSize = 56.sp)
        Text(
            text = stringResource(R.string.contacts_permission_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = stringResource(
                if (permanentlyDenied) {
                    R.string.contacts_permission_denied_body
                } else {
                    R.string.contacts_permission_body
                },
            ),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Button(
            onClick = if (permanentlyDenied) onOpenSettings else onAllow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
        ) {
            Text(
                text = stringResource(
                    if (permanentlyDenied) {
                        R.string.permission_open_settings
                    } else {
                        R.string.contacts_permission_allow
                    },
                ),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

// ── Shared row ────────────────────────────────────────────────────────────────

@Composable
private fun ContactSearchRow(
    contact: DeviceContact,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ContactAvatar(photoUri = contact.photoUri, name = contact.name)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = contact.phoneNumber,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_call),
            contentDescription = null,
            tint = ColorSpeedDial,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ContactAvatar(
    photoUri: Uri?,
    name: String,
) {
    val initials = name.toInitials().ifEmpty { "#" }

    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ColorSpeedDial),
        ) {
            Text(initials, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun String.toInitials(): String =
    split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
