package com.fangjet.launcher.presentation.home.customize

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.BuildConfig
import com.fangjet.launcher.R
import com.fangjet.launcher.domain.model.FallSensitivity
import com.fangjet.launcher.presentation.common.BigBackButton

// Larger type scale tuned for elderly readability.
private val LABEL_SIZE = 26.sp
private val DESC_SIZE = 18.sp
private val ROW_PADDING = 16.dp

// Bigger, easier-to-hit toggles.
private val SwitchModifier = Modifier.scale(1.3f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeHomeScreen(
    onBack: () -> Unit = {},
    onNavigateToEmergencyContacts: () -> Unit = {},
    onNavigateToConnectFamily: () -> Unit = {},
    onNavigateToFavoritePicker: () -> Unit = {},
    viewModel: CustomizeHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val highContrast by viewModel.highContrastEnabled.collectAsStateWithLifecycle()
    val favoriteState by viewModel.favoriteAppsState.collectAsStateWithLifecycle()
    val homeAppState by viewModel.homeAppState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-check Notification access and default-home status after the user
    // returns from system settings.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshSystemStatus()
        onPauseOrDispose { }
    }

    // The system "set default home" sheet resolves without a lifecycle pause,
    // so re-read the role when its result comes back.
    val homeRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshSystemStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
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
            // ── Section: Appearance ───────────────────────────────────────────
            item {
                SectionHeader(stringResource(R.string.customize_appearance_header))
                ToggleRow(
                    label = stringResource(R.string.customize_high_contrast_label),
                    description = stringResource(R.string.customize_high_contrast_description),
                    checked = highContrast,
                    onCheckedChange = { viewModel.setHighContrastEnabled(it) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            // ── Section: Home buttons ─────────────────────────────────────────
            item {
                SectionHeader(stringResource(R.string.customize_home_buttons_header))
                Text(
                    stringResource(R.string.customize_home_buttons_description),
                    fontSize = DESC_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
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

            // ── Voice command button toggle (absent when the Remote Config
            //    kill switch has withdrawn the feature) ──────────────────────
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

            // ── SOS button toggle (safety flavor only) ────────────────────────
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

            // ── Section: Apps Row (absent when the Remote Config kill switch
            //    has withdrawn the feature) ──────────────────────────────────
            if (favoriteState.featureEnabled) {
                item {
                    Spacer(Modifier.height(28.dp))
                    SectionHeader(stringResource(R.string.customize_my_apps_header))
                    Text(
                        stringResource(R.string.customize_my_apps_description),
                        fontSize = DESC_SIZE,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    ToggleRow(
                        label = stringResource(R.string.customize_my_apps_show),
                        checked = favoriteState.rowEnabled,
                        onCheckedChange = { viewModel.setFavoriteRowEnabled(it) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    if (favoriteState.rowEnabled) {
                        ToggleRow(
                            label = stringResource(R.string.customize_my_apps_auto),
                            description = stringResource(R.string.customize_my_apps_auto_description),
                            checked = favoriteState.isAutomatic,
                            onCheckedChange = { viewModel.setFavoriteAutomatic(it) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        if (!favoriteState.isAutomatic) {
                            Button(
                                onClick = onNavigateToFavoritePicker,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .heightIn(min = 64.dp),
                            ) {
                                Text(
                                    stringResource(R.string.customize_my_apps_choose),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        ToggleRow(
                            label = stringResource(R.string.customize_my_apps_badges),
                            description = stringResource(R.string.customize_my_apps_badges_description),
                            checked = favoriteState.badgesEnabled,
                            onCheckedChange = { viewModel.setBadgesEnabled(it) },
                        )
                        if (favoriteState.badgesEnabled && !favoriteState.badgeAccessGranted) {
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .heightIn(min = 64.dp),
                            ) {
                                Text(
                                    stringResource(R.string.customize_my_apps_grant_access),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }

            // ── Sections: Fall Detection + Emergency Contacts (safety only) ───
            if (BuildConfig.SAFETY_FEATURES) {
                item {
                    Spacer(Modifier.height(28.dp))
                    SectionHeader(stringResource(R.string.customize_fall_detection_header))
                    Text(
                        stringResource(R.string.customize_fall_detection_description),
                        fontSize = DESC_SIZE,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    FallDetectionCard(
                        enabled = state.fallDetectionEnabled,
                        sensitivity = state.fallSensitivity,
                        onToggle = { viewModel.setFallDetectionEnabled(it) },
                        onSensitivity = { viewModel.setFallSensitivity(it) },
                    )

                    Spacer(Modifier.height(28.dp))
                }

                item {
                    SectionHeader(stringResource(R.string.customize_emergency_contacts_header))
                    Text(
                        stringResource(R.string.customize_emergency_contacts_description),
                        fontSize = DESC_SIZE,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Button(
                        onClick = onNavigateToEmergencyContacts,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_call),
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(end = 8.dp),
                            tint = Color.White,
                        )
                        Text(
                            stringResource(R.string.customize_manage_emergency_contacts),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            // ── Section: Family (safety flavor only for the initial Play
            //    release — caregiver pairing ships alongside SOS) ──────────
            if (BuildConfig.SAFETY_FEATURES) {
                item {
                    SectionHeader(stringResource(R.string.customize_family_header))
                    Text(
                        stringResource(R.string.customize_family_description),
                        fontSize = DESC_SIZE,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Button(
                        onClick = onNavigateToConnectFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                    ) {
                        Text(
                            stringResource(R.string.customize_connect_family),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.heightIn(min = 28.dp))
                }
            }

            // ── Section: Home app ─────────────────────────────────────────────
            // Both directions of the default-home role: an always-available way
            // out of EasyLink (Play launcher policy: never obstruct switching),
            // and a way in when another launcher currently holds the role.
            item {
                SectionHeader(stringResource(R.string.customize_home_app_header))
                Text(
                    when {
                        homeAppState.isDefault ->
                            stringResource(R.string.customize_home_app_description)

                        homeAppState.currentHomeLabel != null ->
                            stringResource(
                                R.string.customize_home_app_not_default,
                                homeAppState.currentHomeLabel!!,
                            )

                        else -> stringResource(R.string.customize_home_app_not_default_unknown)
                    },
                    fontSize = DESC_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                if (homeAppState.isDefault) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                    ) {
                        Text(
                            stringResource(R.string.customize_change_home_app),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val roleManager = context.getSystemService(RoleManager::class.java)
                                homeRoleLauncher.launch(
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                                )
                            } else {
                                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                    ) {
                        Text(
                            stringResource(R.string.customize_make_home_app),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            // ── Section: About ────────────────────────────────────────────────
            // Google Play's User Data policy requires a privacy policy link
            // inside the app, not just on the store listing.
            item {
                SectionHeader(stringResource(R.string.customize_about_header))
                Text(
                    stringResource(R.string.customize_privacy_policy_description),
                    fontSize = DESC_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                OutlinedButton(
                    onClick = {
                        // No browser on the device is survivable; the URL is
                        // also on the store listing.
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://easylinkcare.com/privacy".toUri()),
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                ) {
                    Text(
                        stringResource(R.string.customize_privacy_policy),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    stringResource(R.string.customize_app_version, BuildConfig.VERSION_NAME),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 28.dp),
                )
            }
        }
    }
}

// ── Toggle row ────────────────────────────────────────────────────────────────

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ROW_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = LABEL_SIZE,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                Text(
                    text = description,
                    fontSize = DESC_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.size(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = SwitchModifier,
        )
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
            .padding(20.dp),
    ) {
        // Enable / disable row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.customize_fall_enable),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (enabled) {
                    Text(
                        stringResource(R.string.customize_fall_active),
                        fontSize = 17.sp,
                        color = Color(0xFF81C784),
                    )
                } else {
                    Text(
                        stringResource(R.string.customize_fall_tap_to_enable),
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = SwitchModifier,
            )
        }

        // Sensitivity — only visible when enabled
        if (enabled) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(R.string.customize_fall_sensitivity),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FallSensitivity.entries.forEach { level ->
                    val selected = level == sensitivity
                    OutlinedButton(
                        onClick = { onSensitivity(level) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                    ) {
                        Text(
                            stringResource(level.shortLabelRes),
                            fontSize = 17.sp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(sensitivity.labelRes),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}
