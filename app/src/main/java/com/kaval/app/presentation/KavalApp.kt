package com.kaval.app.presentation

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kaval.app.core.components.BottomNavItems
import com.kaval.app.core.components.KavalBottomNavBar
import com.kaval.app.core.theme.KavalTheme
import com.kaval.app.data.sms.SmsDeliveryStatusReceiver
import com.kaval.app.domain.model.TrustedContact
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavalApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomRoutes = BottomNavItems.map { it.route }.toSet()
    var pendingEmergencyAfterPermission by remember { mutableStateOf(false) }
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

    fun prepareAndSendRealEmergency() {
        viewModel.prepareEmergency(
            isDemo = false,
            smsStatus = "queued",
            permissionStatus = "granted",
            refreshLocation = true
        ) { alertId, preparedState, contacts ->
            if (contacts.isEmpty()) {
                Toast.makeText(context, "No valid trusted contacts. Emergency logged locally.", Toast.LENGTH_LONG).show()
            } else {
                sendSmsAlerts(context, preparedState, alertId, contacts) { contactId ->
                    viewModel.markSmsFailed(alertId, contactId)
                }
            }
            enterEmergencyMode()
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingEmergencyAfterPermission) {
            if (granted) {
                prepareAndSendRealEmergency()
            } else {
                Toast.makeText(context, "SMS permission denied. Emergency logged locally.", Toast.LENGTH_LONG).show()
                viewModel.prepareEmergency(
                    isDemo = false,
                    smsStatus = "permission_denied",
                    permissionStatus = "denied",
                    errorReason = "SMS permission denied"
                ) { _, _, _ -> enterEmergencyMode() }
            }
            pendingEmergencyAfterPermission = false
        }
    }

    fun triggerEmergencyFlow() {
        if (state.demoMode) {
            Toast.makeText(context, "Demo Mode: SOS simulated. No SMS sent.", Toast.LENGTH_LONG).show()
            viewModel.prepareEmergency(
                isDemo = true,
                smsStatus = "demo_blocked",
                permissionStatus = "not_required"
            ) { _, _, _ -> enterEmergencyMode() }
            return
        }

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSmsPermission) {
            prepareAndSendRealEmergency()
        } else {
            pendingEmergencyAfterPermission = true
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    KavalTheme(settings = state.appearance) {
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
                        onSettings = { navController.navigate(KavalRoutes.Settings) },
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
}

private fun sendSmsAlerts(
    context: Context,
    state: KavalUiState,
    alertId: Long,
    contacts: List<TrustedContact>,
    onImmediateFailure: (Long) -> Unit
) {
    try {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        val message = buildSmsAlertMessage(state)
        contacts.forEach { contact ->
            try {
                val parts = smsManager.divideMessage(message)
                val sentIntents = ArrayList<PendingIntent>()
                val deliveredIntents = ArrayList<PendingIntent>()
                parts.indices.forEach { partIndex ->
                    sentIntents += smsStatusPendingIntent(
                        context, SmsDeliveryStatusReceiver.ACTION_SMS_SENT,
                        alertId, contact.id, partIndex, parts.size
                    )
                    deliveredIntents += smsStatusPendingIntent(
                        context, SmsDeliveryStatusReceiver.ACTION_SMS_DELIVERED,
                        alertId, contact.id, partIndex, parts.size
                    )
                }
                smsManager.sendMultipartTextMessage(
                    contact.phoneNumber, null, parts, sentIntents, deliveredIntents
                )
            } catch (_: RuntimeException) {
                onImmediateFailure(contact.id)
            }
        }
        Toast.makeText(context, "SOS SMS queued for ${contacts.size} trusted contacts.", Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "SMS permission is required for offline SOS.", Toast.LENGTH_LONG).show()
        contacts.forEach { onImmediateFailure(it.id) }
    } catch (_: RuntimeException) {
        Toast.makeText(context, "Could not send SMS. Emergency logged locally.", Toast.LENGTH_LONG).show()
        contacts.forEach { onImmediateFailure(it.id) }
    }
}

private fun smsStatusPendingIntent(
    context: Context,
    action: String,
    alertId: Long,
    contactId: Long,
    partIndex: Int,
    partCount: Int
): PendingIntent {
    val intent = Intent(context, SmsDeliveryStatusReceiver::class.java).apply {
        this.action = action
        putExtra(SmsDeliveryStatusReceiver.EXTRA_ALERT_ID, alertId)
        putExtra(SmsDeliveryStatusReceiver.EXTRA_CONTACT_ID, contactId)
        putExtra(SmsDeliveryStatusReceiver.EXTRA_FINAL_PART, partIndex == partCount - 1)
    }
    val requestCode = listOf(action, alertId, contactId, partIndex).hashCode()
    return PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun buildSmsAlertMessage(state: KavalUiState): String {
    val locationBlock = state.locationState.location?.let { location ->
        val coordinates = String.format(
            java.util.Locale.US,
            "%.6f, %.6f",
            location.latitude,
            location.longitude
        )
        "Coordinates: $coordinates\nLocation: ${location.mapsLink}"
    } ?: "Location unavailable - GPS off or no signal.\nPlease try calling me immediately."

    return """
        Emergency Alert from Kaval.
        I may need help.

        $locationBlock

        Time:
        ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}

        Sent via Kaval.
    """.trimIndent()
}
