package com.tuckercr.ezlauncher.presentation.speeddial.add

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.domain.model.DeviceContact
import com.tuckercr.ezlauncher.ui.theme.ColorSpeedDial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpeedDialScreen(
    onBack: () -> Unit,
    viewModel: AddSpeedDialViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveResult) {
        when (val result = state.saveResult) {
            is AddSpeedDialUiState.SaveResult.Success -> onBack()
            is AddSpeedDialUiState.SaveResult.AlreadyAdded -> {
                snackbarHostState.showSnackbar("Already in Speed Dial")
                viewModel.onSaveResultConsumed()
            }

            is AddSpeedDialUiState.SaveResult.InvalidContact -> {
                snackbarHostState.showSnackbar("Invalid contact — missing name or number")
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
                title = { Text("Add Speed Dial", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_home), contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Mode tabs ─────────────────────────────────────────────────
            TabRow(selectedTabIndex = if (state.isManualEntry) 1 else 0) {
                Tab(
                    selected = !state.isManualEntry,
                    onClick = { if (state.isManualEntry) viewModel.onToggleManualEntry() },
                    text = { Text("Search Contacts", fontSize = 15.sp) },
                )
                Tab(
                    selected = state.isManualEntry,
                    onClick = { if (!state.isManualEntry) viewModel.onToggleManualEntry() },
                    text = { Text("Enter Manually", fontSize = 15.sp) },
                )
            }

            if (state.isManualEntry) {
                ManualEntryPanel(state = state, viewModel = viewModel)
            } else {
                SearchPanel(state = state, viewModel = viewModel)
            }
        }
    }
}

// ── Manual entry panel ────────────────────────────────────────────────────────

@Composable
private fun ManualEntryPanel(
    state: AddSpeedDialUiState,
    viewModel: AddSpeedDialViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Type a name and phone number to add them directly to Speed Dial.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.manualName,
            onValueChange = { viewModel.onManualNameChanged(it) },
            label = { Text("Name") },
            placeholder = { Text("e.g. Dr. Smith") },
            isError = state.manualNameError != null,
            supportingText = state.manualNameError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.manualPhone,
            onValueChange = { viewModel.onManualPhoneChanged(it) },
            label = { Text("Phone number") },
            placeholder = { Text("e.g. 555-123-4567") },
            isError = state.manualPhoneError != null,
            supportingText = state.manualPhoneError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { viewModel.onManualSave() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Add to Speed Dial", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Contact search panel ──────────────────────────────────────────────────────

@Composable
private fun SearchPanel(
    state: AddSpeedDialUiState,
    viewModel: AddSpeedDialViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text("Search contacts") },
            placeholder = { Text("Type a name…") },
            singleLine = true,
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
                        text = "No contacts found for \"${state.searchQuery}\"",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.contacts.isEmpty() -> {
                    Text(
                        text = "Start typing to search your contacts",
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
    val initials = name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "#" }

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
