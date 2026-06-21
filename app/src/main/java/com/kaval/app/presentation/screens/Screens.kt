package com.kaval.app.presentation.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kaval.app.core.components.EmptyState
import com.kaval.app.core.components.KavalActivityCard
import com.kaval.app.core.components.KavalContactCard
import com.kaval.app.core.components.KavalGlassCard
import com.kaval.app.core.components.KavalPrimaryButton
import com.kaval.app.core.components.KavalRiskCard
import com.kaval.app.core.components.KavalSOSButton
import com.kaval.app.core.components.KavalSecondaryButton
import com.kaval.app.core.components.KavalSectionHeader
import com.kaval.app.core.components.KavalStatusBadge
import com.kaval.app.core.components.QuickAction
import com.kaval.app.core.theme.KavalColors
import com.kaval.app.domain.model.AppearanceSettings
import com.kaval.app.domain.model.EmergencyAlert
import com.kaval.app.domain.model.LocationPermissionLevel
import com.kaval.app.domain.model.LocationStatus
import com.kaval.app.domain.model.TrustedContact
import com.kaval.app.domain.model.UserProfile
import com.kaval.app.presentation.KavalUiState
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun KavalScreen(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = {
            item { Spacer(Modifier.height(10.dp)) }
            content()
            item { Spacer(Modifier.height(18.dp)) }
        }
    )
}

@Composable
fun HomeScreen(
    state: KavalUiState,
    onSos: () -> Unit,
    onSettings: () -> Unit,
    onFakeCall: () -> Unit,
    onShareLocation: () -> Unit,
    onGuardianModeChange: (Boolean) -> Unit,
    onPassiveSafetyChange: (Boolean) -> Unit,
    onStartJourney: () -> Unit,
    onBoarded: () -> Unit,
    onReached: () -> Unit
) {
    val context = LocalContext.current
    KavalScreen {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Menu, contentDescription = "Open settings", tint = MaterialTheme.colorScheme.onBackground)
                }
                Column {
                    Text("KAVAL", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text("Personal safety companion", color = KavalColors.Muted)
                }
                Spacer(Modifier.weight(1f))
                if (state.demoMode) KavalStatusBadge("DEMO", KavalColors.Trust)
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader(
                    if (state.safetyStatus.locationSharingActive) "Emergency mode active" else "Safety tools ready",
                    if (state.safetyStatus.locationSharingActive) "Kaval is tracking the active SOS state." else "Check readiness before you travel."
                )
                KavalStatusBadge(
                    text = if (state.safetyStatus.locationSharingActive) "Location Sharing ON" else "Location Sharing OFF",
                    color = if (state.safetyStatus.locationSharingActive) KavalColors.Safe else KavalColors.Muted
                )
            }
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                KavalSOSButton(onActivate = onSos)
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("Safety readiness")
                Text("Location: ${state.locationState.readinessLabel()}")
                Text(
                    "SMS: ${if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) "Ready" else "Permission needed"}"
                )
                Text("Trusted Contacts: ${state.contacts.size}")
                Text("Mode: ${if (state.demoMode) "Demo" else "Real"}")
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("Guardian Mode", state.journeyStatus)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Guardian monitoring", fontWeight = FontWeight.Bold)
                        Text(if (state.guardianModeActive) "In-app session: ON" else "In-app session: OFF", color = KavalColors.Muted)
                    }
                    Switch(state.guardianModeActive, onGuardianModeChange)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Passive Safety Mode", fontWeight = FontWeight.Bold)
                        Text(if (state.passiveSafetyActive) "Visible monitoring state enabled" else "Manual safety only", color = KavalColors.Muted)
                    }
                    Switch(state.passiveSafetyActive, onPassiveSafetyChange)
                }
                KavalStatusBadge(
                    text = if (state.passiveSafetyActive) "Passive Safety Active" else "Passive Safety Idle",
                    color = if (state.passiveSafetyActive) KavalColors.Safe else KavalColors.Muted
                )
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("Journey Timeline", "Before / During / After")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Before", "During", "After").forEach { phase ->
                        KavalStatusBadge(
                            text = phase,
                            color = if (state.journeyPhase == phase) KavalColors.Trust else KavalColors.Muted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(if (state.journeyActive) "Live ETA: 18 min" else "Start a journey before boarding.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KavalPrimaryButton("Start Journey", onStartJourney, Modifier.weight(1f))
                    KavalSecondaryButton("Arrived Safely", onReached, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KavalSecondaryButton(
                        "I've boarded",
                        {
                            onBoarded()
                            Toast.makeText(context, "Boarding update queued for guardian", Toast.LENGTH_SHORT).show()
                        },
                        Modifier.weight(1f)
                    )
                    KavalSecondaryButton(
                        "I've reached",
                        {
                            onReached()
                            Toast.makeText(context, "Reached safely update queued", Toast.LENGTH_SHORT).show()
                        },
                        Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Fake Call", Icons.Default.Call, Modifier.weight(1f), onFakeCall)
                QuickAction("GPS Status", Icons.Default.LocationOn, Modifier.weight(1f)) {
                    Toast.makeText(
                        context,
                        if (state.locationState.location != null) "Live location is ready" else "Refresh GPS before sharing",
                        Toast.LENGTH_SHORT
                    ).show()
                    onShareLocation()
                }
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("Practical Escapes")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KavalSecondaryButton(
                        "Quick Exit Script",
                        { Toast.makeText(context, "Excuse message queued for trusted contact", Toast.LENGTH_SHORT).show() },
                        Modifier.weight(1f)
                    )
                    KavalSecondaryButton(
                        "Google Share",
                        {
                            val shareText = """
                                Kaval safety update:
                                I am sharing my current safety status.

                                Location:
                                ${state.locationState.location?.mapsLink ?: "Location unavailable. Please call me if needed."}

                                Sent via Kaval.
                            """.trimIndent()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share safety update"))
                            Toast.makeText(context, "Choose Messages, Gmail, or another app", Toast.LENGTH_SHORT).show()
                        },
                        Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("Recent Activity")
                Text(state.alerts.firstOrNull()?.let { "Last alert: ${it.status}" } ?: "Last alert: None")
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("Safety permissions")
                Text("Location: ${state.locationState.readinessLabel()}")
                Text("SMS: ${if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) "Ready" else "Permission needed"}")
                Text("Kaval does not request contacts or background-location access.")
            }
        }
    }
}

@Composable
fun MapScreen(
    state: KavalUiState,
    onBack: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRefreshLocation: () -> Unit
) {
    val context = LocalContext.current
    val locationState = state.locationState
    KavalScreen {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("GPS Status", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Supports SOS, Journey, and Guardian Mode", color = KavalColors.Muted)
                }
            }
        }
        item {
            KavalGlassCard {
                KavalStatusBadge(
                    locationState.status.displayLabel(),
                    locationState.status.statusColor()
                )
                Text(locationState.message, color = KavalColors.Muted)
                locationState.location?.let { location ->
                    location.accuracyMeters?.let { accuracy ->
                        Text("Accuracy", color = KavalColors.Muted)
                        Text("Approximately ${accuracy.toInt()} metres", fontWeight = FontWeight.Bold)
                    }
                    Text("Last updated", color = KavalColors.Muted)
                    Text(formatLocationAge(location.timestampMillis), fontWeight = FontWeight.Bold)
                    Text(
                        if (locationState.permissionLevel == LocationPermissionLevel.PRECISE) {
                            "Precise location permission active"
                        } else {
                            "Approximate location permission active"
                        }
                    )
                }
                if (locationState.status == LocationStatus.PERMISSION_NEEDED) {
                    Text("Kaval uses location only when you request safety features. Background location is not requested.")
                    KavalPrimaryButton(
                        "Allow Location",
                        onRequestLocationPermission,
                        Modifier.fillMaxWidth()
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KavalSecondaryButton("Refresh", onRefreshLocation, Modifier.weight(1f))
                        if (locationState.location != null) {
                            KavalPrimaryButton(
                                "Open in Maps",
                                { openInGoogleMaps(context, locationState.location.mapsLink) },
                                Modifier.weight(1f)
                            )
                        }
                    }
                    if (locationState.status == LocationStatus.UNAVAILABLE) {
                        KavalSecondaryButton(
                            "Open Location Settings",
                            { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                            Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("Emergency location link")
                Text(
                    if (locationState.location != null) {
                        "Ready to attach to SOS SMS and safety updates."
                    } else {
                        "No location link is available yet. SOS will still open and explain that location is unavailable."
                    }
                )
                Text("Kaval keeps only the latest location in memory. It does not store continuous movement history.", color = KavalColors.Muted)
            }
        }
    }
}

private fun LocationStatus.statusColor(): Color {
    return when (this) {
        LocationStatus.LIVE -> KavalColors.Safe
        LocationStatus.APPROXIMATE, LocationStatus.STALE -> KavalColors.Warning
        LocationStatus.PERMISSION_NEEDED, LocationStatus.UNAVAILABLE -> KavalColors.Emergency
        LocationStatus.WAITING_FOR_GPS -> KavalColors.Trust
    }
}

private fun openInGoogleMaps(context: android.content.Context, mapsLink: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsLink)))
}

private fun com.kaval.app.domain.model.KavalLocationState.readinessLabel(): String {
    return when (status) {
        LocationStatus.LIVE -> "Ready"
        LocationStatus.APPROXIMATE -> "Approximate"
        LocationStatus.STALE -> "Stale"
        LocationStatus.WAITING_FOR_GPS -> "Waiting"
        LocationStatus.PERMISSION_NEEDED -> "Permission needed"
        LocationStatus.UNAVAILABLE -> "Unavailable"
    }
}

private fun LocationStatus.displayLabel(): String {
    return when (this) {
        LocationStatus.PERMISSION_NEEDED -> "Permission Needed"
        LocationStatus.WAITING_FOR_GPS -> "Waiting for GPS"
        LocationStatus.LIVE -> "Live Location Active"
        LocationStatus.APPROXIMATE -> "Approximate Location Only"
        LocationStatus.STALE -> "Location Stale"
        LocationStatus.UNAVAILABLE -> "Location Unavailable"
    }
}

private fun formatLocationAge(timestampMillis: Long): String {
    val ageSeconds = ((System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L) / 1_000L)
    return when {
        ageSeconds < 10 -> "Just now"
        ageSeconds < 60 -> "$ageSeconds seconds ago"
        ageSeconds < 3_600 -> "${ageSeconds / 60} minutes ago"
        else -> "Over an hour ago"
    }
}

@Composable
fun ContactsScreen(contacts: List<TrustedContact>, onSave: (TrustedContact) -> Unit, onDelete: (TrustedContact) -> Unit) {
    var editing by remember { mutableStateOf<TrustedContact?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add contact")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(10.dp)) }
            item { KavalSectionHeader("Trusted Contacts", "Manual entry for MVP. No contacts permission required.") }
            if (contacts.isEmpty()) {
                item { EmptyState("No trusted contacts yet. Add someone you trust before presenting the SOS flow.") }
            } else {
                items(contacts, key = { it.id }) { contact ->
                    KavalContactCard(
                        contact = contact,
                        onEdit = { editing = contact; showDialog = true },
                        onDelete = { onDelete(contact) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showDialog) {
        ContactDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ContactDialog(initial: TrustedContact?, onDismiss: () -> Unit, onSave: (TrustedContact) -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phoneNumber.orEmpty()) }
    var relationship by remember { mutableStateOf(initial?.relationship.orEmpty()) }
    var primary by remember { mutableStateOf(initial?.isPrimary ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add contact" else "Edit contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone number") }, singleLine = true)
                OutlinedTextField(relationship, { relationship = it }, label = { Text("Relationship") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(primary, { primary = it })
                    Text("Primary contact")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        TrustedContact(
                            id = initial?.id ?: 0,
                            name = name.ifBlank { "Trusted Contact" },
                            phoneNumber = phone.ifBlank { "Not set" },
                            relationship = relationship.ifBlank { "Trusted person" },
                            isPrimary = primary
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ActivityLogScreen(
    alerts: List<EmergencyAlert>,
    retentionDays: Int,
    onRetentionChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(buildSafetyLogCsv(alerts))
                }
            }.onSuccess {
                Toast.makeText(context, "Safety Logs exported", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Could not export Safety Logs", Toast.LENGTH_LONG).show()
            }
        }
    }
    KavalScreen {
        item { KavalSectionHeader("Safety Logs", "Verified emergency, location, and SMS outcomes.") }
        item {
            KavalGlassCard {
                Text(
                    "Logs older than ${if (retentionDays == 90) "3 months" else "4 weeks"} are automatically cleared for your privacy.",
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KavalSecondaryButton("4 weeks", { onRetentionChange(28) }, Modifier.weight(1f))
                    KavalSecondaryButton("3 months", { onRetentionChange(90) }, Modifier.weight(1f))
                }
                KavalPrimaryButton(
                    "Export CSV",
                    { exportLauncher.launch("kaval-safety-logs.csv") },
                    Modifier.fillMaxWidth()
                )
            }
        }
        if (alerts.isEmpty()) {
            item { EmptyState("No emergency activity yet.") }
        } else {
            items(alerts, key = { it.id }) { alert -> KavalActivityCard(alert) }
        }
    }
}

private fun buildSafetyLogCsv(alerts: List<EmergencyAlert>): String {
    val header = "time,type,mode,status,location_status,maps_link,sms_status,sent,delivered,failed,contacts_attempted,error"
    val rows = alerts.map { alert ->
        listOf(
            DateFormat.getDateTimeInstance().format(Date(alert.timestamp)),
            alert.type,
            if (alert.isDemo) "Demo" else "Real",
            alert.status,
            alert.locationStatus,
            alert.mapsLink.orEmpty(),
            alert.smsStatus,
            alert.sentCount,
            alert.deliveredCount,
            alert.failedCount,
            alert.contactsAttempted,
            alert.errorReason.orEmpty()
        ).joinToString(",") { csvEscape(it.toString()) }
    }
    return (listOf(header) + rows).joinToString("\n")
}

private fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""

@Composable
fun HelplineScreen(contacts: List<TrustedContact>) {
    val context = LocalContext.current
    val nationalHelplines = listOf(
        Triple("112", "Unified emergency response", "Police, fire, rescue, and health services"),
        Triple("100", "Police", "Crime, threat, theft, or immediate police assistance"),
        Triple("101", "Fire", "Fire, gas leak, explosion, or rescue"),
        Triple("102", "National Ambulance Service", "Ambulance and maternal or child health transport"),
        Triple("108", "Emergency ambulance", "Medical emergency service; availability varies by state"),
        Triple("181", "Women Helpline", "Support for women in distress"),
        Triple("1091", "Harassment calls", "Anti-obscene or threatening calls"),
        Triple("1098", "Child Helpline", "Children in distress or at risk"),
        Triple("1930", "Cyber Crime", "Online financial fraud, scams, or digital threats"),
        Triple("1070", "Disaster relief", "Natural calamities and disaster assistance"),
        Triple("139", "Railway assistance", "Railway security and medical assistance"),
        Triple("14416", "Tele-MANAS", "Mental health support and counselling")
    )
    KavalScreen {
        item { KavalSectionHeader("Helplines", "National services. Kaval opens the dialer so you stay in control.") }
        item {
            KavalGlassCard {
                KavalStatusBadge("Emergency", KavalColors.Emergency)
                Text("112 Unified Emergency Response", fontWeight = FontWeight.Bold)
                Text("Use this first for immediate police, fire, rescue, or medical help.", color = KavalColors.Muted)
                KavalPrimaryButton(
                    "Dial 112",
                    { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
                    Modifier.fillMaxWidth(),
                    emergency = true
                )
            }
        }
        item { KavalSectionHeader("National services") }
        items(nationalHelplines, key = { it.first }) { helpline ->
            KavalGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KavalStatusBadge(helpline.first, if (helpline.first == "112") KavalColors.Emergency else KavalColors.Trust)
                    Column(Modifier.weight(1f)) {
                        Text(helpline.second, fontWeight = FontWeight.Bold)
                        Text(helpline.third, color = KavalColors.Muted)
                    }
                    IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${helpline.first}"))) }) {
                        Icon(Icons.Default.Call, contentDescription = "Dial ${helpline.first}")
                    }
                }
            }
        }
        item { KavalSectionHeader("Trusted contacts") }
        if (contacts.isEmpty()) {
            item { EmptyState("Add a trusted contact from Profile before you need one.") }
        } else {
            items(contacts, key = { it.id }) { contact ->
                KavalGlassCard {
                    Text(contact.name, fontWeight = FontWeight.Bold)
                    Text(contact.relationship, color = KavalColors.Muted)
                    KavalSecondaryButton(
                        "Open Dialer",
                        { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(contact.phoneNumber)}"))) },
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: KavalUiState,
    onDemoModeChange: (Boolean) -> Unit,
    onProfile: () -> Unit,
    onAppearance: () -> Unit,
    onFakeCall: () -> Unit
) {
    KavalScreen {
        item { KavalSectionHeader("Settings", "Profile, appearance, safety preferences, demo mode, and about Kaval.") }
        item {
            KavalGlassCard {
                SettingRow("Profile", "Emergency notes and medical context", Icons.Default.Person, onProfile)
                SettingRow("Appearance", "Theme, motion, text size, and live preview", Icons.Default.Palette, onAppearance)
                SettingRow("Fake Call", "Start an internal fake call simulation", Icons.Default.Phone, onFakeCall)
            }
        }
        item {
            KavalGlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Demo Mode", fontWeight = FontWeight.Bold)
                        Text(
                            if (state.demoMode) {
                                "Real SMS is blocked. SOS alerts are simulated only."
                            } else {
                                "Real SMS fallback can send to trusted contacts after permission."
                            },
                            color = KavalColors.Muted
                        )
                    }
                    Switch(checked = state.demoMode, onCheckedChange = onDemoModeChange)
                }
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("About Kaval")
                Text("Kaval is a personal safety companion using on-device GPS, offline SMS, and trusted-contact safety tools.")
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = KavalColors.Muted)
        }
        TextButton(onClick = onClick) { Text("Open") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit,
    onContacts: () -> Unit,
    onSafetyLogs: () -> Unit,
    onSettings: () -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var phone by remember(profile) { mutableStateOf(profile.phoneNumber) }
    var note by remember(profile) { mutableStateOf(profile.emergencyNote) }
    var blood by remember(profile) { mutableStateOf(profile.bloodGroup.orEmpty()) }
    var medical by remember(profile) { mutableStateOf(profile.medicalNote.orEmpty()) }

    Scaffold(topBar = { KavalTopBar("Profile", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                KavalGlassCard {
                    KavalSectionHeader("Safety tools")
                    SettingRow("Trusted Contacts", "People contacted during SOS", Icons.Default.Person, onContacts)
                    SettingRow("Guardian Settings", "Choose sharing and guardian contacts", Icons.Default.LocationOn, onSettings)
                    SettingRow("Safety Logs", "SOS location and SMS outcomes", Icons.Default.Warning, onSafetyLogs)
                    SettingRow("Settings", "Demo mode, appearance, and permissions", Icons.Default.Menu, onSettings)
                }
            }
            item { KavalSectionHeader("Personal information", "Used in emergency messages and your safety profile.") }
            item { OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(phone, { phone = it }, label = { Text("Phone number") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(note, { note = it }, label = { Text("Emergency note") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
            item { OutlinedTextField(blood, { blood = it }, label = { Text("Blood group optional") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(medical, { medical = it }, label = { Text("Medical note optional") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            item {
                KavalPrimaryButton(
                    text = "Save Profile",
                    onClick = {
                        onSave(UserProfile(name, phone, note, blood.ifBlank { null }, medical.ifBlank { null }))
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(settings: AppearanceSettings, onSave: (AppearanceSettings) -> Unit, onBack: () -> Unit) {
    var current by remember(settings) { mutableStateOf(settings) }

    Scaffold(topBar = { KavalTopBar("Appearance", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ChoiceGroup("Theme Mode", listOf("System Default", "Dark", "Light", "Emergency High Contrast"), current.themeMode) { current = current.copy(themeMode = it) } }
            item { ChoiceGroup("Visual Style", listOf("Protective Glass", "Soft Neo", "Minimal Classic", "High Visibility"), current.visualStyle) { current = current.copy(visualStyle = it) } }
            item { ChoiceGroup("SOS Button Style", listOf("Pulse Ring", "Raised Emergency Button + Pulse Ring", "Hold-to-Activate", "Countdown Button"), current.sosButtonStyle) { current = current.copy(sosButtonStyle = it) } }
            item { ChoiceGroup("Color Intensity", listOf("Calm", "Balanced", "High Alert"), current.colorIntensity) { current = current.copy(colorIntensity = it) } }
            item { ChoiceGroup("Motion Effects", listOf("Reduced Motion", "Standard Motion", "Enhanced Motion"), current.motionLevel) { current = current.copy(motionLevel = it) } }
            item { ChoiceGroup("Text Size", listOf("Compact", "Standard", "Large"), current.textSize) { current = current.copy(textSize = it) } }
            item {
                KavalGlassCard {
                    KavalSectionHeader("Live Preview")
                    KavalStatusBadge("Safety status", KavalColors.Safe)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { KavalSOSButton(onActivate = {}) }
                    KavalStatusBadge("Example warning", KavalColors.Warning)
                    KavalStatusBadge("Example safe", KavalColors.Safe)
                }
            }
            item {
                KavalPrimaryButton(
                    text = "Save Appearance",
                    onClick = { onSave(current); onBack() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ChoiceGroup(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    KavalGlassCard {
        KavalSectionHeader(title)
        options.forEach { option ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(option)
                Checkbox(checked = selected == option, onCheckedChange = { if (it) onSelect(option) })
            }
        }
    }
}

@Composable
fun FakeCallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var caller by remember { mutableStateOf("Mom") }
    var delayLabel by remember { mutableStateOf("Immediate") }
    var incoming by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(false) }
    var remainingDelaySeconds by remember { mutableIntStateOf(0) }
    var scheduleGeneration by remember { mutableIntStateOf(0) }
    var ttsReady by remember { mutableStateOf(false) }
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    val callers = listOf("Mom", "Brother", "Friend", "Emergency Contact")
    val delays = listOf("Immediate", "10 sec", "30 sec", "1 min")

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
        textToSpeech = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            textToSpeech = null
        }
    }

    LaunchedEffect(scheduleGeneration) {
        if (remainingDelaySeconds > 0) {
            while (remainingDelaySeconds > 0) {
                delay(1_000L)
                remainingDelaySeconds -= 1
            }
            incoming = true
        }
    }

    DisposableEffect(incoming, answered) {
        var ringtone: Ringtone? = null
        val vibrator = context.kavalVibrator()
        if (incoming && !answered) {
            val toneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, toneUri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone?.isLooping = true
            ringtone?.play()
            val vibrationPattern = longArrayOf(0, 700, 500, 700, 1_200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(vibrationPattern, 0)
            }
        }
        onDispose {
            ringtone?.stop()
            vibrator?.cancel()
        }
    }

    LaunchedEffect(answered, ttsReady) {
        if (answered && ttsReady) {
            delay(700)
            val script = when (caller) {
                "Mom" -> "Hi, I need you to come home now. I am waiting for you."
                "Brother" -> "Hey, where are you? I need to pick you up right away."
                "Friend" -> "Hi, something urgent came up. Please meet me outside now."
                else -> "Hello. Please move to a safe place and call me back immediately."
            }
            textToSpeech?.speak(script, TextToSpeech.QUEUE_FLUSH, null, "kaval_fake_call")
        }
    }

    Scaffold(topBar = { KavalTopBar("Fake Call", onBack) }) { padding ->
        if (incoming) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07111F))
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(72.dp), tint = KavalColors.Safe)
                    Text(if (answered) "Call in progress" else "Incoming call", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(caller, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    if (answered) {
                        Text(if (ttsReady) "Caller audio is playing" else "Preparing caller audio...", color = Color.White.copy(alpha = 0.78f))
                        KavalPrimaryButton("End Call", { incoming = false; answered = false }, emergency = true)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            KavalPrimaryButton("Answer", { answered = true }, emergency = false)
                            KavalPrimaryButton("Decline", { incoming = false; answered = false }, emergency = true)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { ChoiceGroup("Caller Name", callers, caller) { caller = it } }
                item { ChoiceGroup("Delay", delays, delayLabel) { delayLabel = it } }
                item {
                    KavalPrimaryButton(
                        text = if (remainingDelaySeconds > 0) "Fake Call Scheduled" else "Start Fake Call",
                        onClick = {
                            remainingDelaySeconds = when (delayLabel) {
                                "10 sec" -> 10
                                "30 sec" -> 30
                                "1 min" -> 60
                                else -> 0
                            }
                            if (remainingDelaySeconds == 0) {
                                incoming = true
                            } else {
                                scheduleGeneration += 1
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (remainingDelaySeconds > 0) {
                    item {
                        KavalGlassCard {
                            Text("Incoming fake call will appear in $remainingDelaySeconds seconds.")
                            KavalSecondaryButton(
                                "Cancel Scheduled Call",
                                {
                                    scheduleGeneration += 1
                                    remainingDelaySeconds = 0
                                },
                                Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun android.content.Context.kavalVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
}

@Composable
fun EmergencyCountdownScreen(onCancel: () -> Unit, onTriggered: () -> Unit) {
    var count by remember { mutableIntStateOf(3) }
    LaunchedEffect(Unit) {
        while (count > 0) {
            delay(1000)
            count -= 1
        }
        onTriggered()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KavalColors.Emergency)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Emergency alert in", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(count.coerceAtLeast(0).toString(), color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
            Text("Cancel if this was accidental.", color = Color.White.copy(alpha = 0.9f))
            KavalSecondaryButton("Cancel", onCancel)
        }
    }
}

@Composable
fun EmergencyModeScreen(state: KavalUiState, onStop: () -> Unit) {
    var mockCallMessage by remember { mutableStateOf<String?>(null) }
    val alert = state.alerts.firstOrNull()

    KavalScreen {
        item {
            KavalGlassCard {
                KavalStatusBadge(if (alert?.isDemo == true) "Demo Alert" else "Emergency Active", KavalColors.Emergency)
                Text(alert?.locationLabel ?: "Checking location", fontWeight = FontWeight.Bold)
                Text("SMS status: ${alert?.smsStatus?.replace('_', ' ') ?: "queued"}")
                Text("Sent: ${alert?.sentCount ?: 0}/${alert?.contactsAttempted ?: 0}")
                Text("Delivered: ${alert?.deliveredCount ?: 0}")
                Text("Failed: ${alert?.failedCount ?: 0}")
                Text("Emergency ID: ${alert?.id ?: "Pending"}")
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader(if (alert?.isDemo == true) "Simulated alert" else "SOS location")
                Text(alert?.mapsLink ?: "Location unavailable. Please try calling immediately.")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KavalPrimaryButton("Stop Emergency Mode", onStop, Modifier.weight(1f), emergency = true)
                KavalSecondaryButton(
                    "Call Trusted Contact",
                    {
                        mockCallMessage = state.contacts.firstOrNull()
                            ?.let { "Mock call ready for ${it.name}. No real call was placed." }
                            ?: "No trusted contact is available for a mock call."
                    },
                    Modifier.weight(1f)
                )
            }
        }
        mockCallMessage?.let { message ->
            item {
                KavalGlassCard {
                    KavalSectionHeader("Mock call action")
                    Text(message)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KavalTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}
