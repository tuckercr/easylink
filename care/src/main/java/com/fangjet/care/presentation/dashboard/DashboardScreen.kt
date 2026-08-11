package com.fangjet.care.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.care.R
import com.fangjet.care.presentation.common.PrivacyPolicyLink
import com.fangjet.care.presentation.theme.Good
import com.fangjet.care.presentation.theme.Teal

/**
 * The caregiver's home: who they're looking after, and what they can manage.
 * Visual language follows the pitch mockups — teal header band, white cards,
 * status chips.
 */
@Composable
fun DashboardScreen(
    onOpenContacts: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Teal header band ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Teal)
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 26.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.dashboard_greeting),
                color = androidx.compose.ui.graphics.Color.White
                    .copy(alpha = 0.85f),
                fontSize = 15.sp,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Elder card ────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Teal, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.elderName
                                .take(1)
                                .uppercase()
                                .ifEmpty { "?" },
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = state.elderName.ifEmpty {
                                stringResource(R.string.dashboard_loading)
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Good, CircleShape),
                            )
                            Text(
                                text = stringResource(R.string.dashboard_connected),
                                fontSize = 13.sp,
                                color = Good,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.padding(2.dp))
            Text(
                text = stringResource(R.string.dashboard_manage_header),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )

            // ── Feature rows ──────────────────────────────────────────────────
            FeatureRow(
                emoji = "🆘",
                title = stringResource(R.string.dashboard_contacts_title),
                subtitle = if (state.contactCount > 0) {
                    pluralStringResource(
                        R.plurals.dashboard_contacts_count,
                        state.contactCount,
                        state.contactCount,
                    )
                } else {
                    stringResource(R.string.dashboard_contacts_none)
                },
                enabled = true,
                onClick = onOpenContacts,
            )
            FeatureRow(
                emoji = "💊",
                title = stringResource(R.string.dashboard_meds_title),
                subtitle = stringResource(R.string.dashboard_coming_soon),
                enabled = false,
                onClick = {},
            )
            FeatureRow(
                emoji = "🔔",
                title = stringResource(R.string.dashboard_alerts_title),
                subtitle = stringResource(R.string.dashboard_coming_soon),
                enabled = false,
                onClick = {},
            )

            Spacer(Modifier.weight(1f))
            PrivacyPolicyLink(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun FeatureRow(
    emoji: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(
                alpha = if (enabled) 1f else 0.6f,
            ),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = emoji, fontSize = 26.sp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (enabled) 1f else 0.5f,
                    ),
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
            if (enabled) {
                Text(
                    text = "›",
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
    }
}
