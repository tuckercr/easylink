package com.tuckercr.ezlauncher.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.data.preferences.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: OnboardingPreferences,
) : ViewModel() {
    fun markComplete() {
        viewModelScope.launch { prefs.markComplete() }
    }
}

// ── Step model ────────────────────────────────────────────────────────────────

private data class OnboardingStep(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    /** Dangerous permissions to request. Empty = auto-granted / no prompt needed. */
    val permissions: List<String>,
)

private val STEPS: List<OnboardingStep> = buildList {
    add(
        OnboardingStep(
            emoji = "📞",
            titleRes = R.string.onboarding_phone_title,
            descRes = R.string.onboarding_phone_desc,
            permissions = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS),
        ),
    )
    add(
        OnboardingStep(
            emoji = "💬",
            titleRes = R.string.onboarding_sms_title,
            descRes = R.string.onboarding_sms_desc,
            permissions = listOf(Manifest.permission.SEND_SMS),
        ),
    )
    add(
        OnboardingStep(
            emoji = "📍",
            titleRes = R.string.onboarding_location_title,
            descRes = R.string.onboarding_location_desc,
            permissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        ),
    )
    add(
        OnboardingStep(
            emoji = "📷",
            titleRes = R.string.onboarding_camera_title,
            descRes = R.string.onboarding_camera_desc,
            permissions = listOf(Manifest.permission.CAMERA),
        ),
    )
    // POST_NOTIFICATIONS only needs a runtime prompt on API 33+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(
            OnboardingStep(
                emoji = "🔔",
                titleRes = R.string.onboarding_notifications_title,
                descRes = R.string.onboarding_notifications_desc,
                permissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
            ),
        )
    }
    add(
        OnboardingStep(
            emoji = "🎤",
            titleRes = R.string.onboarding_microphone_title,
            descRes = R.string.onboarding_microphone_desc,
            permissions = listOf(Manifest.permission.RECORD_AUDIO),
        ),
    )
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Step-by-step permission onboarding wizard.
 *
 * Each step explains a permission group in plain language and requests it via
 * [ActivityResultContracts.RequestMultiplePermissions]. The user can allow or
 * skip any step; the wizard completes after the last step regardless of which
 * permissions were granted. Individual features handle denied permissions
 * gracefully at point-of-use.
 *
 * [onComplete] is called after the final step — the caller should navigate to
 * the home screen.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    val advance: () -> Unit = {
        if (currentStep < STEPS.lastIndex) {
            currentStep++
        } else {
            viewModel.markComplete()
            onComplete()
        }
    }

    // Single launcher handles every step's permissions — registered once,
    // called with each step's list when the user taps Allow.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        advance()
    }

    val step = STEPS[currentStep]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Progress dots ─────────────────────────────────────────────────
            ProgressDots(total = STEPS.size, current = currentStep)

            Spacer(Modifier.height(40.dp))

            // ── Feature icon ──────────────────────────────────────────────────
            Text(
                text = step.emoji,
                fontSize = 80.sp,
            )

            Spacer(Modifier.height(24.dp))

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text = stringResource(step.titleRes),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            // ── Description ───────────────────────────────────────────────────
            Text(
                text = stringResource(step.descRes),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
            )

            Spacer(Modifier.height(48.dp))

            // ── Allow button ──────────────────────────────────────────────────
            val isLastStep = currentStep == STEPS.lastIndex
            Button(
                onClick = {
                    if (step.permissions.isEmpty()) {
                        advance()
                    } else {
                        permissionLauncher.launch(step.permissions.toTypedArray())
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text(
                    text = if (isLastStep) {
                        stringResource(R.string.onboarding_allow_and_start)
                    } else {
                        stringResource(
                            R.string.onboarding_allow_access,
                        )
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Skip link ─────────────────────────────────────────────────────
            TextButton(onClick = advance) {
                Text(
                    text = if (isLastStep) {
                        stringResource(R.string.onboarding_skip)
                    } else {
                        stringResource(
                            R.string.onboarding_skip_for_now,
                        )
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ── Progress dots ─────────────────────────────────────────────────────────────

@Composable
private fun ProgressDots(
    total: Int,
    current: Int,
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == current) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) active else inactive),
            )
        }
    }
}
