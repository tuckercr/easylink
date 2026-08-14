package com.fangjet.launcher.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fangjet.launcher.data.preferences.OnboardingPreferences
import com.fangjet.launcher.presentation.apps.AppsScreen
import com.fangjet.launcher.presentation.forecast.ForecastScreen
import com.fangjet.launcher.presentation.home.HomeScreen
import com.fangjet.launcher.presentation.home.customize.CustomizeHomeScreen
import com.fangjet.launcher.presentation.home.customize.FavoriteAppsPickerScreen
import com.fangjet.launcher.presentation.magnifier.MagnifierScreen
import com.fangjet.launcher.presentation.medications.MedicationsScreen
import com.fangjet.launcher.presentation.medications.add.AddMedicationScreen
import com.fangjet.launcher.presentation.onboarding.OnboardingScreen
import com.fangjet.launcher.presentation.settings.ConnectFamilyScreen
import com.fangjet.launcher.presentation.settings.EmergencyContactsScreen
import com.fangjet.launcher.presentation.sos.SosCountdownScreen
import com.fangjet.launcher.presentation.speeddial.SpeedDialScreen
import com.fangjet.launcher.presentation.speeddial.add.AddSpeedDialScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// ── Route constants ───────────────────────────────────────────────────────────

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val APPS = "apps"
    const val SPEED_DIAL = "speed_dial"
    const val MEDICATIONS = "medications"
    const val MAGNIFIER = "magnifier"
    const val SOS_COUNTDOWN = "sos_countdown"
    const val ADD_SPEED_DIAL = "add_speed_dial"
    const val EDIT_SPEED_DIAL = "edit_speed_dial/{speedDialId}"
    const val ADD_MEDICATION = "add_medication"
    const val EDIT_MEDICATION = "edit_medication/{medicationId}"
    const val CUSTOMIZE_HOME = "customize_home"
    const val FORECAST = "weather_forecast"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val CONNECT_FAMILY = "connect_family"
    const val FAVORITE_APPS_PICKER = "favorite_apps_picker"

    fun editMedication(id: Long) = "edit_medication/$id"

    fun editSpeedDial(id: Long) = "edit_speed_dial/$id"
}

// ── Startup ViewModel — determines first screen ───────────────────────────────

/**
 * Resolves the start destination before the NavHost is built.
 *
 * [isOnboardingComplete] emits:
 *  - null  → still reading DataStore (show a blank loading screen)
 *  - false → first launch → show onboarding
 *  - true  → returning user → go straight to home
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    prefs: OnboardingPreferences,
) : ViewModel() {
    val isOnboardingComplete: StateFlow<Boolean?> = prefs.isComplete
        .map { it as Boolean? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}

// ── Tremor-safe navigation ────────────────────────────────────────────────────
//
// Shaky hands double-tap. The second tap of a rapid pair used to push the same
// route twice (People over People) or pop two levels on a Back button. While a
// navigation is in flight the current back-stack entry has not yet reached
// RESUMED, so gating on that drops exactly the duplicate taps — and stays safe
// for programmatic calls from a settled screen (e.g. navigate-back after a
// save), where the current entry is RESUMED.

private val NavHostController.isSettled: Boolean
    get() = currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

private fun NavHostController.navigateIfSettled(route: String) {
    if (isSettled) navigate(route)
}

private fun NavHostController.popIfSettled() {
    if (isSettled) popBackStack()
}

// ── Nav host ──────────────────────────────────────────────────────────────────

@Composable
fun EasyLinkNavHost(
    startupViewModel: StartupViewModel = hiltViewModel(),
    pendingNavTarget: String? = null,
    onNavTargetConsumed: () -> Unit = {},
    onHomeScreenResumed: () -> Unit = {},
) {
    val isOnboardingComplete by startupViewModel.isOnboardingComplete
        .collectAsStateWithLifecycle()

    // Show a blank background while DataStore loads (typically < 50 ms)
    if (isOnboardingComplete == null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        return
    }

    // Frozen at first read: the flag flips to true mid-session when the
    // onboarding home-role step persists completion early, and a changing
    // startDestination would rebuild the nav graph out from under the wizard.
    val startDestination = rememberSaveable {
        if (isOnboardingComplete == true) Routes.HOME else Routes.ONBOARDING
    }

    val navController = rememberNavController()

    // ── Navigation logging ────────────────────────────────────────────────────
    DisposableEffect(navController) {
        val listener =
            NavController.OnDestinationChangedListener { _, destination, arguments ->
                val route = destination.route ?: "unknown"
                val args = arguments
                    ?.keySet()
                    // Skip internal navigation keys — they can hold non-String types
                    // (e.g. Intent) which cause a ClassCastException if read as String.
                    ?.filter { !it.startsWith("android-support-nav:") }
                    ?.mapNotNull { key ->
                        @Suppress("DEPRECATION")
                        arguments.get(key)?.let { v -> "$key=$v" }
                    }?.joinToString()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { " [$it]" }
                    ?: ""
                Log.d("Navigation", "→ $route$args")
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    // Navigate to the pending target (e.g. from FallAlertActivity → Emergency Contacts)
    LaunchedEffect(pendingNavTarget) {
        if (pendingNavTarget != null) {
            navController.navigate(pendingNavTarget)
            onNavTargetConsumed()
        }
    }

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            // ── Onboarding (shown once on first launch) ───────────────────────
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }

            // ── Main screens ──────────────────────────────────────────────────
            composable(
                route = Routes.HOME,
                // Custom slide transitions for navigating to/from the All Apps screen.
                exitTransition = {
                    when (targetState.destination.route) {
                        Routes.APPS -> slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(300),
                        )

                        else -> null
                    }
                },
                popEnterTransition = {
                    when (initialState.destination.route) {
                        Routes.APPS -> slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(300),
                        )

                        else -> null
                    }
                },
            ) {
                // Launcher root: intercept back gesture to prevent activity re-launch or reload.
                BackHandler(enabled = true) { /* consume — launcher root, back does nothing */ }
                // Fires when Home is composed while resumed and on each later
                // resume with Home on top — never on inner screens, so the
                // set-as-home prompt can't interrupt a task in progress.
                LifecycleResumeEffect(Unit) {
                    onHomeScreenResumed()
                    onPauseOrDispose { }
                }
                HomeScreen(
                    onNavigateToApps = { navController.navigateIfSettled(Routes.APPS) },
                    onNavigateToMagnifier = { navController.navigateIfSettled(Routes.MAGNIFIER) },
                    onNavigateToSos = { navController.navigateIfSettled(Routes.SOS_COUNTDOWN) },
                    onNavigateToForecast = { navController.navigateIfSettled(Routes.FORECAST) },
                    onNavigateToSpeedDial = { navController.navigateIfSettled(Routes.SPEED_DIAL) },
                    onNavigateToMedications = { navController.navigateIfSettled(Routes.MEDICATIONS) },
                    onNavigateToSettings = { navController.navigateIfSettled(Routes.CUSTOMIZE_HOME) },
                )
            }
            composable(Routes.CUSTOMIZE_HOME) {
                CustomizeHomeScreen(
                    onBack = { navController.popIfSettled() },
                    onNavigateToEmergencyContacts = {
                        navController.navigateIfSettled(Routes.EMERGENCY_CONTACTS)
                    },
                    onNavigateToConnectFamily = {
                        navController.navigateIfSettled(Routes.CONNECT_FAMILY)
                    },
                    onNavigateToFavoritePicker = {
                        navController.navigateIfSettled(Routes.FAVORITE_APPS_PICKER)
                    },
                )
            }
            composable(Routes.FAVORITE_APPS_PICKER) {
                FavoriteAppsPickerScreen(onBack = { navController.popIfSettled() })
            }
            composable(Routes.EMERGENCY_CONTACTS) {
                EmergencyContactsScreen(onBack = { navController.popIfSettled() })
            }
            composable(Routes.CONNECT_FAMILY) {
                ConnectFamilyScreen(onBack = { navController.popIfSettled() })
            }
            composable(
                route = Routes.APPS,
                // Slide in from the right (standard Android push pattern).
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300),
                    )
                },
                // Slide out to the right when the user presses back / the home button.
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300),
                    )
                },
            ) {
                AppsScreen(onBack = { navController.popIfSettled() })
            }
            composable(Routes.SPEED_DIAL) {
                SpeedDialScreen(
                    onBack = { navController.popIfSettled() },
                    onNavigateToAddContact = { navController.navigateIfSettled(Routes.ADD_SPEED_DIAL) },
                    onNavigateToEdit = { id -> navController.navigateIfSettled(Routes.editSpeedDial(id)) },
                )
            }
            composable(
                route = Routes.EDIT_SPEED_DIAL,
                arguments = listOf(
                    navArgument("speedDialId") { type = NavType.LongType },
                ),
            ) {
                AddSpeedDialScreen(
                    onBack = { navController.popIfSettled() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.MEDICATIONS) {
                MedicationsScreen(
                    onBack = { navController.popIfSettled() },
                    onNavigateToAddMedication = { navController.navigateIfSettled(Routes.ADD_MEDICATION) },
                    onNavigateToEditMedication = { id ->
                        navController.navigateIfSettled(Routes.editMedication(id))
                    },
                )
            }
            composable(Routes.MAGNIFIER) {
                MagnifierScreen(onBack = { navController.popIfSettled() })
            }
            composable(Routes.SOS_COUNTDOWN) {
                // Programmatic (countdown finished), not a tap — never drop it.
                SosCountdownScreen(onFinished = { navController.popBackStack() })
            }
            composable(Routes.ADD_SPEED_DIAL) {
                AddSpeedDialScreen(
                    onBack = { navController.popIfSettled() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.ADD_MEDICATION) {
                AddMedicationScreen(
                    onBack = { navController.popIfSettled() },
                    // Post-save pop is programmatic and may fire before the entry
                    // re-reaches RESUMED (permission-dialog resume) — never drop it.
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.FORECAST) {
                ForecastScreen(onBack = { navController.popIfSettled() })
            }
            composable(
                route = Routes.EDIT_MEDICATION,
                arguments = listOf(
                    navArgument("medicationId") { type = NavType.LongType },
                ),
            ) {
                AddMedicationScreen(
                    onBack = { navController.popIfSettled() },
                    // Post-save pop is programmatic and may fire before the entry
                    // re-reaches RESUMED (permission-dialog resume) — never drop it.
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}
