package com.tuckercr.ezlauncher.presentation.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.domain.model.SosResult
import com.tuckercr.ezlauncher.ui.theme.ColorSos

@Composable
fun SosCountdownScreen(
    onFinished: () -> Unit,
    viewModel: SosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate away automatically when cancelled or done
    LaunchedEffect(state) {
        if (state is SosUiState.Cancelled || state is SosUiState.Done) {
            kotlinx.coroutines.delay(2_000)
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000)),
        contentAlignment = Alignment.Center,
    ) {
        when (val s = state) {
            is SosUiState.Counting -> CountingContent(
                secondsRemaining = s.secondsRemaining,
                onCancel = { viewModel.cancel() },
            )
            is SosUiState.Dispatching -> DispatchingContent()
            is SosUiState.Done -> DoneContent(result = s.result)
            is SosUiState.Cancelled -> CancelledContent()
        }
    }
}

@Composable
private fun CountingContent(secondsRemaining: Int, onCancel: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Text(
            text = "SOS",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ColorSos,
            letterSpacing = 8.sp,
        )
        Text(
            text = "Sending in",
            fontSize = 24.sp,
            color = Color.White,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp),
        ) {
            CircularProgressIndicator(
                progress = { secondsRemaining / SosUiState.COUNTDOWN_SECONDS.toFloat() },
                modifier = Modifier.size(160.dp),
                color = ColorSos,
                strokeWidth = 8.dp,
            )
            Text(
                text = "$secondsRemaining",
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorSos,
            )
        }
        Text(
            text = "seconds",
            fontSize = 24.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "CANCEL",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )
        }
    }
}

@Composable
private fun DispatchingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        CircularProgressIndicator(
            color = ColorSos,
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp,
        )
        Text(
            text = "Sending SOS…",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Contacting emergency contacts",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DoneContent(result: SosResult) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        val (icon, iconTint, headline, detail) = when (result) {
            is SosResult.Success -> SosDisplay(
                icon = R.drawable.ic_check,
                tint = Color(0xFF4CAF50),
                headline = "SOS Sent",
                detail = buildSuccessDetail(result),
            )
            is SosResult.PartialSuccess -> SosDisplay(
                icon = R.drawable.ic_check,
                tint = Color(0xFFFFCC80),
                headline = "Partially Sent",
                detail = "SMS sent to ${result.smsRecipients} contact(s).\n${result.reason}",
            )
            is SosResult.NoContactsConfigured -> SosDisplay(
                icon = R.drawable.ic_call,
                tint = Color(0xFFEF9A9A),
                headline = "No Contacts",
                detail = "No emergency contacts configured. Please add one in Settings.",
            )
            is SosResult.Failure -> SosDisplay(
                icon = R.drawable.ic_call,
                tint = Color(0xFFEF9A9A),
                headline = "SOS Failed",
                detail = result.reason,
            )
        }

        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(80.dp),
        )
        Text(
            text = headline,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = detail,
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CancelledContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Text(
            text = "Cancelled",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "SOS was not sent",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

private data class SosDisplay(
    val icon: Int,
    val tint: Color,
    val headline: String,
    val detail: String,
)

private fun buildSuccessDetail(result: SosResult.Success): String = buildString {
    result.calledContact?.let { append("Called ${it.name}.\n") }
    if (result.smsRecipients > 0) append("SMS sent to ${result.smsRecipients} contact(s).")
    if (result.locationShared) append("\nLocation shared.")
}
