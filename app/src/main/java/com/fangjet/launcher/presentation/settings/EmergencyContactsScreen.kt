package com.fangjet.launcher.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.R
import com.fangjet.launcher.domain.model.EmergencyContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    onBack: () -> Unit,
    viewModel: EmergencySettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // One-shot snackbar events
    LaunchedEffect(state) {
        if (state is EmergencySettingsUiState.Ready) {
            (state as EmergencySettingsUiState.Ready).snackbarMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onSnackbarShown()
            }
        }
    }

    // Add / edit dialog
    if (state is EmergencySettingsUiState.Ready) {
        val ready = state as EmergencySettingsUiState.Ready
        ready.editingContact?.let { contact ->
            ContactEditDialog(
                contact = contact,
                dialogKey = ready.dialogKey,
                validationError = ready.validationError,
                onSave = { name, phone, isPrimary ->
                    viewModel.onSaveContact(name, phone, isPrimary)
                },
                onDismiss = { viewModel.onDismissDialog() },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.emergency_contacts_title),
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
        when (val s = state) {
            is EmergencySettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is EmergencySettingsUiState.Ready -> {
                if (s.contacts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        SosPermissionBanner()
                        EmptyContactsState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            onAdd = { viewModel.onAddContactClicked() },
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        item { SosPermissionBanner() }
                        item {
                            Text(
                                text = stringResource(R.string.emergency_contacts_description),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        items(s.contacts, key = { it.id }) { contact ->
                            ContactCard(
                                contact = contact,
                                onEdit = { viewModel.onEditContactClicked(contact) },
                                onDelete = { viewModel.onDeleteContact(contact) },
                            )
                        }

                        item {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.onAddContactClicked() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB71C1C),
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp),
                            ) {
                                Text(
                                    stringResource(R.string.emergency_contacts_add_another),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── SOS permission readiness banner ─────────────────────────────────────────────

/**
 * Warns (and offers a one-tap fix) when the permissions SOS needs to text and
 * call contacts aren't granted. Self-hides once everything is granted, and
 * re-checks after the permission request returns. Location is included because
 * it enriches the SOS text with the person's coordinates.
 */
@Composable
private fun SosPermissionBanner() {
    val context = LocalContext.current
    val required = remember {
        listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }

    fun missing(): List<String> =
        required.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    var missingPerms by remember { mutableStateOf(missing()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { missingPerms = missing() }

    if (missingPerms.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF5D2A0E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.sos_perm_banner_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFCC80),
        )
        Text(
            text = stringResource(R.string.sos_perm_banner_body),
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f),
        )
        Button(
            onClick = { launcher.launch(missingPerms.toTypedArray()) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE65100),
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(
                stringResource(R.string.sos_perm_grant),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyContactsState(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_call),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFEF9A9A),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.emergency_contacts_empty_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.emergency_contacts_empty_body),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB71C1C),
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp),
        ) {
            Text(
                stringResource(R.string.emergency_contacts_add),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Contact card ──────────────────────────────────────────────────────────────

@Composable
private fun ContactCard(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onEdit)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = contact.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (contact.isPrimary) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFB71C1C))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_contacts_primary_badge),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = contact.phoneNumber,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.emergency_contacts_tap_to_edit),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Delete button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable(onClick = onDelete),
        ) {
            Text(
                text = "×",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

// ── Add / edit dialog ─────────────────────────────────────────────────────────

@Composable
private fun ContactEditDialog(
    contact: EmergencyContact,
    dialogKey: Int,
    validationError: String?,
    onSave: (name: String, phone: String, isPrimary: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    // Use dialogKey (not contact.id) as the remember key so fields always reset
    // when the dialog opens — even when "Add Contact" is opened twice in a row,
    // where contact.id would be 0 both times and wouldn't trigger a reset.
    var name by remember(dialogKey) { mutableStateOf(contact.name) }
    var phone by remember(dialogKey) { mutableStateOf(contact.phoneNumber) }
    var isPrimary by remember(dialogKey) { mutableStateOf(contact.isPrimary) }
    val isNew = contact.id == 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) {
                    stringResource(R.string.emergency_contacts_dialog_add_title)
                } else {
                    stringResource(
                        R.string.emergency_contacts_dialog_edit_title,
                    )
                },
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    placeholder = { Text(stringResource(R.string.emergency_contacts_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.field_phone_number)) },
                    placeholder = { Text(stringResource(R.string.field_phone_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.emergency_contacts_primary_label),
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                        )
                        Text(
                            text = stringResource(R.string.emergency_contacts_primary_body),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                    )
                }
                if (validationError != null) {
                    Text(
                        text = validationError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, phone, isPrimary) }) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), fontSize = 16.sp)
            }
        },
    )
}
