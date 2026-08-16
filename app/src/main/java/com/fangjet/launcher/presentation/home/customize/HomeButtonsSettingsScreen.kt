package com.fangjet.launcher.presentation.home.customize

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.BuildConfig
import com.fangjet.launcher.R
import com.fangjet.launcher.presentation.common.BigBackButton

/**
 * Child screen holding every Home screen button toggle (Phone, Text, Camera,
 * … plus Voice and SOS), reached from the "Manage Buttons" row on the main
 * Settings screen. Split out on its own because the combined list ran to
 * 13+ rows — too long to sit inline without burying every other Settings
 * section below it.
 *
 * Reuses [CustomizeHomeViewModel] (same package) rather than a dedicated
 * ViewModel: the state and toggle functions this screen needs already live
 * there, backed by DataStore, so both screens always agree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeButtonsSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: CustomizeHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.customize_home_buttons_header),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                windowInsets = WindowInsets(0),
            )
        },
        bottomBar = { BigBackButton(onClick = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.customize_home_buttons_description),
                    fontSize = DESC_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
            }

            items(state.buttons, key = { it.button.name }) { item ->
                ToggleRow(
                    label = stringResource(item.labelRes),
                    checked = item.isEnabled,
                    onCheckedChange = { viewModel.toggle(item.button, it) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            // Voice and SOS aren't HomeButton entries (their enable/disable
            // logic is special-cased — Remote Config kill switch, permanent-
            // denial hide, safety-flavor-only), but they read as home screen
            // buttons just the same, so they stay grouped with the list above.
            if (state.voiceFeatureEnabled) {
                item {
                    ToggleRow(
                        label = stringResource(R.string.customize_voice_button_label),
                        description = stringResource(R.string.customize_voice_button_description),
                        checked = state.voiceButtonEnabled,
                        onCheckedChange = { viewModel.setVoiceButtonEnabled(it) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            if (BuildConfig.SAFETY_FEATURES) {
                item {
                    ToggleRow(
                        label = stringResource(R.string.customize_sos_button_label),
                        description = stringResource(R.string.customize_sos_button_description),
                        checked = state.sosButtonEnabled,
                        onCheckedChange = { viewModel.setSosButtonEnabled(it) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}
