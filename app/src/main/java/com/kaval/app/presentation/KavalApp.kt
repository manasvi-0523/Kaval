package com.kaval.app.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kaval.app.core.components.BottomNavItems
import com.kaval.app.core.components.KavalBottomNavBar
import com.kaval.app.core.theme.KavalTheme
import com.kaval.app.presentation.navigation.KavalRoutes
import com.kaval.app.presentation.screens.ActivityLogScreen
import com.kaval.app.presentation.screens.AppearanceScreen
import com.kaval.app.presentation.screens.ContactsScreen
import com.kaval.app.presentation.screens.EmergencyCountdownScreen
import com.kaval.app.presentation.screens.EmergencyModeScreen
import com.kaval.app.presentation.screens.FakeCallScreen
import com.kaval.app.presentation.screens.HomeScreen
import com.kaval.app.presentation.screens.MapScreen
import com.kaval.app.presentation.screens.ProfileScreen
import com.kaval.app.presentation.screens.SettingsScreen

@Composable
fun KavalApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomRoutes = BottomNavItems.map { it.route }.toSet()

    KavalTheme(themeMode = state.appearance.themeMode) {
        Scaffold(
            bottomBar = {
                if (currentRoute in bottomRoutes) {
                    KavalBottomNavBar(currentRoute, BottomNavItems) { route ->
                        navController.navigate(route) {
                            popUpTo(KavalRoutes.Home)
                            launchSingleTop = true
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = KavalRoutes.Home,
                modifier = Modifier.padding(padding)
            ) {
                composable(KavalRoutes.Home) {
                    HomeScreen(
                        state = state,
                        onSos = { navController.navigate(KavalRoutes.Countdown) },
                        onFakeCall = { navController.navigate(KavalRoutes.FakeCall) },
                        onShareLocation = { navController.navigate(KavalRoutes.Map) }
                    )
                }
                composable(KavalRoutes.Map) { MapScreen(state) }
                composable(KavalRoutes.Contacts) {
                    ContactsScreen(
                        contacts = state.contacts,
                        onSave = viewModel::saveContact,
                        onDelete = viewModel::deleteContact
                    )
                }
                composable(KavalRoutes.Activity) { ActivityLogScreen(state.alerts) }
                composable(KavalRoutes.Settings) {
                    SettingsScreen(
                        state = state,
                        onDemoModeChange = viewModel::setDemoMode,
                        onProfile = { navController.navigate(KavalRoutes.Profile) },
                        onAppearance = { navController.navigate(KavalRoutes.Appearance) },
                        onFakeCall = { navController.navigate(KavalRoutes.FakeCall) }
                    )
                }
                composable(KavalRoutes.Profile) {
                    ProfileScreen(profile = state.profile, onSave = viewModel::saveProfile, onBack = { navController.popBackStack() })
                }
                composable(KavalRoutes.Appearance) {
                    AppearanceScreen(settings = state.appearance, onSave = viewModel::saveAppearance, onBack = { navController.popBackStack() })
                }
                composable(KavalRoutes.FakeCall) { FakeCallScreen(onBack = { navController.popBackStack() }) }
                composable(KavalRoutes.Countdown) {
                    EmergencyCountdownScreen(
                        onCancel = { navController.popBackStack() },
                        onTriggered = {
                            viewModel.triggerEmergency()
                            navController.navigate(KavalRoutes.EmergencyMode) {
                                popUpTo(KavalRoutes.Home)
                            }
                        }
                    )
                }
                composable(KavalRoutes.EmergencyMode) {
                    EmergencyModeScreen(
                        state = state,
                        onStop = {
                            viewModel.stopEmergency()
                            navController.navigate(KavalRoutes.Home) {
                                popUpTo(KavalRoutes.Home) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}
