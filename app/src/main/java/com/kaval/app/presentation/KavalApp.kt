package com.kaval.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kaval.app.core.theme.KavalTheme
import com.kaval.app.presentation.navigation.KavalRoutes
import com.kaval.app.presentation.screens.ActivityLogScreen
import com.kaval.app.presentation.screens.AppearanceScreen
import com.kaval.app.presentation.screens.ContactsScreen
import com.kaval.app.presentation.screens.EmergencyCountdownScreen
import com.kaval.app.presentation.screens.EmergencyModeScreen
import com.kaval.app.presentation.screens.FakeCallScreen
import com.kaval.app.presentation.screens.HomeScreen
import com.kaval.app.presentation.screens.HelplineScreen
import com.kaval.app.presentation.screens.MapScreen
import com.kaval.app.presentation.screens.PermissionExplanationContent
import com.kaval.app.presentation.screens.ProfileScreen
import com.kaval.app.presentation.screens.SettingsScreen
import com.kaval.app.service.AudioRecordingService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavalApp(
    viewModel: AppViewModel = viewModel(),
    sosViewModel: SosViewModel = viewModel(),
    openEmergencyMode: Boolean = false,
    onEmergencyIntentConsumed: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var pendingSmsPermissionGranted by remember { mutableStateOf(false) }
    var showSmsExplanation by remember { mutableStateOf(false) }
    var showAudioExplanation by remember { mutableStateOf(false) }
    var showCoarseLocationExplanation by remember { mutableStateOf(false) }
    var showFineLocationExplanation by remember { mutableStateOf(false) }
    var locationPermissionDenied by remember { mutableStateOf(false) }
    var locationExplanationOffered by remember { mutableStateOf(false) }

    val fineLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshLocationPermission()
        locationPermissionDenied = false
        if (granted || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.refreshLocation()
        }
    }

    val coarseLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshLocationPermission()
        locationPermissionDenied = !granted
        if (granted) showFineLocationExplanation = true
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLocation()
    }

    LaunchedEffect(currentRoute, state.locationState.permissionLevel) {
        if (currentRoute == KavalRoutes.Map &&
            state.locationState.permissionLevel == com.kaval.app.domain.model.LocationPermissionLevel.NONE &&
            !locationExplanationOffered
        ) {
            locationExplanationOffered = true
            showCoarseLocationExplanation = true
        }
    }

    fun beginLocationPermissionFlow() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                showCoarseLocationExplanation = true
            }
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                showFineLocationExplanation = true
            }
            else -> viewModel.refreshLocation()
        }
    }

    fun enterEmergencyMode() {
        navController.navigate(KavalRoutes.EmergencyMode) {
            popUpTo(KavalRoutes.Home)
        }
    }

    LaunchedEffect(openEmergencyMode) {
        if (openEmergencyMode) {
            enterEmergencyMode()
            onEmergencyIntentConsumed()
        }
    }

    fun triggerSos(smsPermissionGranted: Boolean, audioPermissionGranted: Boolean) {
        sosViewModel.trigger(state, smsPermissionGranted, audioPermissionGranted) {
            enterEmergencyMode()
        }
    }

    fun hasRecordingPermissions(): Boolean {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        return hasAudio && hasNotifications
    }

    fun continueAfterSmsDecision(smsPermissionGranted: Boolean) {
        pendingSmsPermissionGranted = smsPermissionGranted
        if (hasRecordingPermissions()) {
            triggerSos(smsPermissionGranted, audioPermissionGranted = true)
        } else {
            showAudioExplanation = true
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true
        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            grants[Manifest.permission.POST_NOTIFICATIONS] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        triggerSos(
            smsPermissionGranted = pendingSmsPermissionGranted,
            audioPermissionGranted = audioGranted && notificationGranted
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        continueAfterSmsDecision(granted)
    }

    fun triggerEmergencyFlow() {
        if (state.demoMode) {
            triggerSos(smsPermissionGranted = false, audioPermissionGranted = false)
            return
        }

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSmsPermission) {
            continueAfterSmsDecision(smsPermissionGranted = true)
        } else {
            showSmsExplanation = true
        }
    }
    KavalTheme(settings = state.appearance) {
            NavHost(
                navController = navController,
                startDestination = KavalRoutes.Home
            ) {
                composable(KavalRoutes.Home) {
                    HomeScreen(
                        state = state,
                        onSos = { navController.navigate(KavalRoutes.Countdown) },
                        onSettings = { navController.navigate(KavalRoutes.Settings) },
                        onProfile = { navController.navigate(KavalRoutes.Profile) },
                        onHelpline = { navController.navigate(KavalRoutes.Helpline) },
                        onFakeCall = { navController.navigate(KavalRoutes.FakeCall) },
                        onShareLocation = { navController.navigate(KavalRoutes.Map) },
                        onGuardianModeChange = viewModel::setGuardianMode,
                        onPassiveSafetyChange = viewModel::setPassiveSafety,
                        onStartJourney = viewModel::startJourney,
                        onBoarded = viewModel::markBoarded,
                        onReached = viewModel::markReached
                    )
                }
                composable(KavalRoutes.Map) {
                    MapScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        locationAccessDenied = locationPermissionDenied,
                        onRequestLocationPermission = { beginLocationPermissionFlow() },
                        onRefreshLocation = viewModel::refreshLocation
                    )
                }
                composable(KavalRoutes.Helpline) {
                    HelplineScreen(state.contacts)
                }
                composable(KavalRoutes.Contacts) {
                    ContactsScreen(
                        contacts = state.contacts,
                        onSave = viewModel::saveContact,
                        onDelete = viewModel::deleteContact
                    )
                }
                composable(KavalRoutes.Activity) {
                    ActivityLogScreen(
                        alerts = state.alerts,
                        retentionDays = state.logRetentionDays,
                        onRetentionChange = viewModel::setLogRetentionDays
                    )
                }
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
                    ProfileScreen(
                        profile = state.profile,
                        onSave = viewModel::saveProfile,
                        onBack = { navController.popBackStack() },
                        onContacts = { navController.navigate(KavalRoutes.Contacts) },
                        onSafetyLogs = { navController.navigate(KavalRoutes.Activity) },
                        onSettings = { navController.navigate(KavalRoutes.Settings) }
                    )
                }
                composable(KavalRoutes.Appearance) {
                    AppearanceScreen(settings = state.appearance, onSave = viewModel::saveAppearance, onBack = { navController.popBackStack() })
                }
                composable(KavalRoutes.FakeCall) { FakeCallScreen(onBack = { navController.popBackStack() }) }
                composable(KavalRoutes.Countdown) {
                    EmergencyCountdownScreen(
                        onCancel = { navController.popBackStack() },
                        onTriggered = { triggerEmergencyFlow() }
                    )
                }
                composable(KavalRoutes.EmergencyMode) {
                    EmergencyModeScreen(
                        state = state,
                        onStop = {
                            AudioRecordingService.stop(context)
                            viewModel.stopEmergency()
                            navController.navigate(KavalRoutes.Home) {
                                popUpTo(KavalRoutes.Home) { inclusive = true }
                            }
                        },
                    )
                }
            }
    }

    if (showCoarseLocationExplanation) {
        ModalBottomSheet(onDismissRequest = { showCoarseLocationExplanation = false }) {
            PermissionExplanationContent(
                title = "Allow approximate location?",
                reason = "Kaval needs your location to show your position and share it with guardians during emergencies.",
                onAllow = {
                    showCoarseLocationExplanation = false
                    coarseLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                onDismiss = { showCoarseLocationExplanation = false }
            )
        }
    }

    if (showFineLocationExplanation) {
        ModalBottomSheet(onDismissRequest = { showFineLocationExplanation = false }) {
            PermissionExplanationContent(
                title = "Enable precise location?",
                reason = "Precise location helps Kaval place you accurately on the map and attach a more useful location to SOS alerts.",
                onAllow = {
                    showFineLocationExplanation = false
                    fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onDismiss = {
                    showFineLocationExplanation = false
                    viewModel.refreshLocation()
                }
            )
        }
    }

    if (showSmsExplanation) {
        ModalBottomSheet(onDismissRequest = {
            showSmsExplanation = false
            continueAfterSmsDecision(smsPermissionGranted = false)
        }) {
            PermissionExplanationContent(
                title = "Allow emergency SMS?",
                reason = "Kaval uses SMS only when a real SOS is triggered. If denied, Emergency Mode still opens, but trusted contacts will not receive the cellular alert.",
                onAllow = {
                    showSmsExplanation = false
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                },
                onDismiss = {
                    showSmsExplanation = false
                    continueAfterSmsDecision(smsPermissionGranted = false)
                }
            )
        }
    }

    if (showAudioExplanation) {
        ModalBottomSheet(onDismissRequest = {
            showAudioExplanation = false
            triggerSos(pendingSmsPermissionGranted, audioPermissionGranted = false)
        }) {
            PermissionExplanationContent(
                title = "Record local SOS evidence?",
                reason = "Kaval records audio only after a real SOS so you have local evidence. A persistent notification stays visible, recording stops after 10 minutes, and the file never leaves this device unless you share it.",
                onAllow = {
                    showAudioExplanation = false
                    val permissions = buildList {
                        add(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                    audioPermissionLauncher.launch(permissions)
                },
                onDismiss = {
                    showAudioExplanation = false
                    triggerSos(pendingSmsPermissionGranted, audioPermissionGranted = false)
                }
            )
        }
    }}
