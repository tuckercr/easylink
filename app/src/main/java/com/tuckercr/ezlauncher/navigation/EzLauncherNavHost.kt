package com.tuckercr.ezlauncher.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.presentation.apps.AppsScreen
import com.tuckercr.ezlauncher.presentation.clock.ClockScreen
import com.tuckercr.ezlauncher.presentation.home.HomeScreen
import com.tuckercr.ezlauncher.presentation.inbox.InboxScreen
import com.tuckercr.ezlauncher.presentation.magnifier.MagnifierScreen
import com.tuckercr.ezlauncher.presentation.medications.MedicationsScreen
import com.tuckercr.ezlauncher.presentation.medications.add.AddMedicationScreen
import com.tuckercr.ezlauncher.presentation.sos.SosCountdownScreen
import com.tuckercr.ezlauncher.presentation.speeddial.SpeedDialScreen
import com.tuckercr.ezlauncher.presentation.speeddial.add.AddSpeedDialScreen

// ── Route constants ───────────────────────────────────────────────────────────

object Routes {
    const val HOME            = "home"
    const val APPS            = "apps"
    const val SPEED_DIAL      = "speed_dial"
    const val INBOX           = "inbox"
    const val MEDICATIONS     = "medications"
    const val MAGNIFIER       = "magnifier"
    const val SOS_COUNTDOWN   = "sos_countdown"
    const val ADD_SPEED_DIAL  = "add_speed_dial"
    const val ADD_MEDICATION  = "add_medication"
    const val EDIT_MEDICATION = "edit_medication/{medicationId}"
    const val CLOCK           = "clock"

    fun editMedication(id: Long) = "edit_medication/$id"
}

// Bottom-nav tabs (order determines display position left→right)
private data class BottomNavItem(
    val route: String,
    val labelRes: String,
    val iconRes: Int,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME,        "Home",       R.drawable.ic_home),
    BottomNavItem(Routes.SPEED_DIAL,  "Speed Dial", R.drawable.ic_call),
    BottomNavItem(Routes.INBOX,       "Inbox",      R.drawable.ic_sms),
    BottomNavItem(Routes.MEDICATIONS, "Meds",       R.drawable.ic_pill),
    BottomNavItem(Routes.CLOCK,       "Clock",      R.drawable.ic_alarm),
)

private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()

// ── Nav host ──────────────────────────────────────────────────────────────────

@Composable
fun EzLauncherNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show the bottom bar on the four top-level tabs
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination
                            ?.hierarchy
                            ?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = item.labelRes,
                                )
                            },
                            label = { Text(item.labelRes, fontSize = 11.sp) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Routes.HOME,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToApps      = { navController.navigate(Routes.APPS) },
                    onNavigateToMagnifier = { navController.navigate(Routes.MAGNIFIER) },
                    onNavigateToSos       = { navController.navigate(Routes.SOS_COUNTDOWN) },
                )
            }
            composable(Routes.APPS) {
                AppsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SPEED_DIAL) {
                SpeedDialScreen(
                    onNavigateToAddContact = { navController.navigate(Routes.ADD_SPEED_DIAL) }
                )
            }
            composable(Routes.INBOX) {
                InboxScreen()
            }
            composable(Routes.MEDICATIONS) {
                MedicationsScreen(
                    onNavigateToAddMedication  = { navController.navigate(Routes.ADD_MEDICATION) },
                    onNavigateToEditMedication = { id -> navController.navigate(Routes.editMedication(id)) },
                )
            }
            composable(Routes.MAGNIFIER) {
                MagnifierScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SOS_COUNTDOWN) {
                SosCountdownScreen(onFinished = { navController.popBackStack() })
            }
            composable(Routes.ADD_SPEED_DIAL) {
                AddSpeedDialScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADD_MEDICATION) {
                AddMedicationScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CLOCK) {
                ClockScreen()
            }
            composable(
                route = Routes.EDIT_MEDICATION,
                arguments = listOf(
                    navArgument("medicationId") { type = NavType.LongType }
                ),
            ) {
                AddMedicationScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
