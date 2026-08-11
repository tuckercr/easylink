package com.fangjet.care.presentation.common

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.fangjet.care.R

/**
 * Opens the privacy policy in the browser. Google Play's User Data policy
 * requires a policy link inside the app, so this sits on both the pairing
 * screen (the first thing a new user or reviewer sees) and the dashboard.
 */
@Composable
fun PrivacyPolicyLink(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, "https://easylinkcare.com/privacy".toUri()),
                )
            }
        },
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.privacy_policy),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}
