package com.fangjet.launcher.presentation.onboarding

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.BuildConfig
import com.fangjet.launcher.R
import com.fangjet.launcher.data.preferences.OnboardingPreferences
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
    /** True for the final step, which requests the HOME role instead of a permission. */
    val isHomeRoleStep: Boolean = false,
    /**
     * True for the opening step: explains the app and that permission asks
     * follow. No skip link (there is nothing to skip — the button just
     * continues) and a privacy-policy link in its place.
     */
    val isWelcomeStep: Boolean = false,
)

/**
 * Taps closer together than this are treated as one — tremor double-taps
 * arrive well inside it, while a deliberate next-step tap comes later.
 */
private const val TAP_DEBOUNCE_MS = 750L

private val STEPS: List<OnboardingStep> = buildList {
    add(
        OnboardingStep(
            emoji = "👋",
            titleRes = R.string.onboarding_welcome_title,
            descRes = R.string.onboarding_welcome_desc,
            permissions = emptyList(),
            isWelcomeStep = true,
        ),
    )
    add(
        OnboardingStep(
            emoji = "📞",
            titleRes = R.string.onboarding_phone_title,
            descRes = R.string.onboarding_phone_desc,
            permissions = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS),
        ),
    )
    // SMS (SOS alerts) exists only in the safety flavor — the standard manifest
    // has no SEND_SMS, so requesting it would be auto-denied.
    if (BuildConfig.SAFETY_FEATURES) {
        add(
            OnboardingStep(
                emoji = "💬",
                titleRes = R.string.onboarding_sms_title,
                descRes = R.string.onboarding_sms_desc,
                permissions = listOf(Manifest.permission.SEND_SMS),
            ),
        )
    }
    add(
        OnboardingStep(
            emoji = "📍",
            titleRes = R.string.onboarding_location_title,
            descRes = R.string.onboarding_location_desc,
            permissions = buildList {
                // Fine location is SOS-only (safety flavor); weather needs coarse.
                if (BuildConfig.SAFETY_FEATURES) add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
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
    // Finale: become the home screen. Not a permission — the HOME role, via
    // the system's "set default home app" sheet.
    add(
        OnboardingStep(
            emoji = "🏠",
            titleRes = R.string.onboarding_home_title,
            descRes = R.string.onboarding_home_desc,
            permissions = emptyList(),
            isHomeRoleStep = true,
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

    // ── Double-tap guard ──────────────────────────────────────────────────────
    // Shaky hands double-tap. Without this, the second tap of a rapid pair can
    // land before the system dialog opens — on this step's button again, or
    // (after advance) on the NEXT step's button, which sits at the same screen
    // position — answering or skipping a step the user never read.
    //
    // Two layers: a shared time debounce for every action button, and a hard
    // lock while a permission/role request is in flight (its dialog can take
    // longer than any debounce window to appear; the result callback unlocks).
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var awaitingSystemDialog by remember { mutableStateOf(false) }
    val guarded: (() -> Unit) -> () -> Unit = { action ->
        {
            val now = SystemClock.elapsedRealtime()
            if (!awaitingSystemDialog && now - lastTapAt >= TAP_DEBOUNCE_MS) {
                lastTapAt = now
                action()
            }
        }
    }

    // Single launcher handles every step's permissions — registered once,
    // called with each step's list when the user taps Allow.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        awaitingSystemDialog = false
        advance()
    }

    // The home-role step: advance whatever the outcome — like permissions,
    // every step is skippable and the launcher works without the role.
    val context = LocalContext.current
    val homeRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        awaitingSystemDialog = false
        advance()
    }
    val requestHomeRole: () -> Unit = {
        // Persist completion BEFORE launching the role request: granting the
        // HOME role makes the system restart the launcher task on the spot,
        // so the result callback (and any markComplete inside it) may never
        // run — the relaunched app would start onboarding over from step one.
        viewModel.markComplete()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true) {
                advance()
            } else {
                awaitingSystemDialog = true
                homeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            }
        } else {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
            advance()
        }
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
            // Scrolls when the content outgrows the screen (long welcome text
            // at a large accessibility font scale); centered by the Box when
            // it fits, so the usual layout is unchanged.
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
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
                onClick = guarded {
                    when {
                        step.isHomeRoleStep -> requestHomeRole()
                        step.permissions.isEmpty() -> advance()
                        else -> {
                            awaitingSystemDialog = true
                            permissionLauncher.launch(step.permissions.toTypedArray())
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
            ) {
                Text(
                    text = when {
                        step.isWelcomeStep -> stringResource(R.string.onboarding_get_started)
                        step.isHomeRoleStep -> stringResource(R.string.onboarding_home_button)
                        isLastStep -> stringResource(R.string.onboarding_allow_and_start)
                        else -> stringResource(R.string.onboarding_allow_access)
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (step.isWelcomeStep) {
                // ── Privacy policy link (welcome step only) ───────────────────
                TextButton(
                    onClick = guarded {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://easylinkcare.com/privacy".toUri()),
                            )
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.customize_privacy_policy),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
            } else {
                // ── Skip link ─────────────────────────────────────────────────
                TextButton(onClick = guarded(advance)) {
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
