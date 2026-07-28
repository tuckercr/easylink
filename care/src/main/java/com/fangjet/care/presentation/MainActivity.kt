package com.fangjet.care.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fangjet.care.presentation.contacts.ContactsScreen
import com.fangjet.care.presentation.dashboard.DashboardScreen
import com.fangjet.care.presentation.pairing.PairingScreen
import com.fangjet.care.presentation.theme.EasyLinkCareTheme
import dagger.hilt.android.AndroidEntryPoint

object CareRoutes {
    const val DASHBOARD = "dashboard"
    const val CONTACTS = "contacts"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EasyLinkCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CareRoot()
                }
            }
        }
    }
}

/**
 * Root routing keyed off the stored linkId: unpaired shows the pairing flow,
 * and the moment redemption stores a linkId this recomposes into the app —
 * which is also what dismisses the pairing screen after a successful connect.
 */
@androidx.compose.runtime.Composable
private fun CareRoot(viewModel: CareRootViewModel = hiltViewModel()) {
    val linkId by viewModel.linkId.collectAsStateWithLifecycle()

    when (linkId) {
        // Distinguish "still loading DataStore" from "definitely unpaired" so
        // a paired user doesn't see a flash of the pairing screen on launch.
        CareRootViewModel.LOADING -> Unit
        null -> PairingScreen()
        else -> {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = CareRoutes.DASHBOARD) {
                composable(CareRoutes.DASHBOARD) {
                    DashboardScreen(
                        onOpenContacts = { navController.navigate(CareRoutes.CONTACTS) },
                    )
                }
                composable(CareRoutes.CONTACTS) {
                    ContactsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
