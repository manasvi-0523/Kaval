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
import com.kaval.app.presentation.screens.ProfileScreen
import com.kaval.app.presentation.screens.SettingsScreen

@Composable
fun KavalApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomRoutes = BottomNavItems.map { it.route }.toSet()
    var pendingEmergencyAfterPermission by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshLocationPermission()
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.refreshLocation()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLocation()
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
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
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
    return """
        Emergency Alert from Kaval.
        I may need help.

        Location:
        ${state.locationState.location?.mapsLink ?: "Location unavailable.\nPlease try calling me immediately."}

        Time:
        ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}

        Sent via Kaval.
    """.trimIndent()
}
