package com.fangjet.launcher.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.R
import com.fangjet.launcher.presentation.common.BigBackButton

/**
 * Shows the 6-digit pairing code, huge, for a family member to type into
 * EasyLink Care. Everything is sized for the elderly user who is reading the
 * code out loud (possibly over the phone).
 */
@Composable
fun ConnectFamilyScreen(
    onBack: () -> Unit,
    viewModel: ConnectFamilyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = { BigBackButton(onClick = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val s = state) {
                is ConnectFamilyUiState.Preparing -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(24.dp))
                    BigBody(stringResource(R.string.connect_family_preparing))
                }

                is ConnectFamilyUiState.ShowingCode -> {
                    BigTitle(stringResource(R.string.connect_family_title))
                    Spacer(Modifier.height(12.dp))
                    BigBody(stringResource(R.string.connect_family_instructions))
                    Spacer(Modifier.height(36.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(20.dp),
                            ).padding(vertical = 34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = s.code,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    BigBody(
                        stringResource(
                            R.string.connect_family_expires,
                            s.secondsLeft / 60,
                            s.secondsLeft % 60,
                        ),
                    )
                }

                is ConnectFamilyUiState.Connected -> {
                    Text(text = "✅", fontSize = 84.sp)
                    Spacer(Modifier.height(20.dp))
                    BigTitle(stringResource(R.string.connect_family_connected_title))
                    Spacer(Modifier.height(12.dp))
                    BigBody(stringResource(R.string.connect_family_connected_body))
                }

                is ConnectFamilyUiState.Expired,
                is ConnectFamilyUiState.Error,
                -> {
                    BigTitle(
                        stringResource(
                            if (s is ConnectFamilyUiState.Expired) {
                                R.string.connect_family_expired_title
                            } else {
                                R.string.connect_family_error_title
                            },
                        ),
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = { viewModel.start() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 76.dp),
                    ) {
                        Text(
                            stringResource(R.string.connect_family_retry),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BigTitle(text: String) {
    Text(
        text = text,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun BigBody(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
        lineHeight = 28.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
