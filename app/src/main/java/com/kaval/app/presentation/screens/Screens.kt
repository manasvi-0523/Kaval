package com.kaval.app.presentation.screens

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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.kaval.app.domain.model.TrustedContact
import com.kaval.app.domain.model.UserProfile
import com.kaval.app.presentation.KavalUiState
import kotlinx.coroutines.delay

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
fun HomeScreen(state: KavalUiState, onSos: () -> Unit, onFakeCall: () -> Unit, onShareLocation: () -> Unit) {
    KavalScreen {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("KAVAL", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text("Personal safety companion", color = KavalColors.Muted)
                }
                if (state.demoMode) KavalStatusBadge("Demo Mode", KavalColors.Trust)
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader(state.safetyStatus.status, "Risk level: ${state.safetyStatus.riskLevel}")
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
                Text("Trusted Contacts: ${state.contacts.size} active", fontWeight = FontWeight.SemiBold)
                Text("Location Sharing: ${if (state.safetyStatus.locationSharingActive) "ON" else "OFF"}")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Fake Call", Icons.Default.Call, Modifier.weight(1f), onFakeCall)
                QuickAction("Share Location", Icons.Default.LocationOn, Modifier.weight(1f), onShareLocation)
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
                KavalSectionHeader("Permissions for real mode")
                Text("Location permission will be required for real live location sharing.")
                Text("Notification permission will be required for real emergency alerts.")
                Text("Contacts permission is optional and not used in this MVP.")
            }
        }
    }
}

@Composable
fun MapScreen(state: KavalUiState) {
    KavalScreen {
        item {
            KavalSectionHeader("Safety Map", "Demo map data")
        }
        item {
            KavalGlassCard {
                Text("Current area", fontWeight = FontWeight.Bold)
                Text("Demo Location")
                KavalStatusBadge("Tracking Active", KavalColors.Trust)
            }
        }
        item { KavalRiskCard("Safe Zone", "Well-lit public route with normal activity.", KavalColors.Safe, Icons.Default.CheckCircle) }
        item { KavalRiskCard("Caution Area", "Lower visibility and fewer public checkpoints.", KavalColors.Warning, Icons.Default.Info) }
        item { KavalRiskCard("High Risk Area", "Avoid route in demo safety data.", KavalColors.Emergency, Icons.Default.Warning) }
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
fun ActivityLogScreen(alerts: List<EmergencyAlert>) {
    KavalScreen {
        item { KavalSectionHeader("Activity Log", "Emergency history and mock alert records.") }
        if (alerts.isEmpty()) {
            item { EmptyState("No emergency activity yet.") }
        } else {
            items(alerts, key = { it.id }) { alert -> KavalActivityCard(alert) }
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
                        Text("No real alerts, contact notifications, SMS, or calls.", color = KavalColors.Muted)
                    }
                    Switch(checked = state.demoMode, onCheckedChange = onDemoModeChange)
                }
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("About Kaval")
                Text("Kaval is a personal safety companion MVP focused on reliable emergency interaction before real services are added.")
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
fun ProfileScreen(profile: UserProfile, onSave: (UserProfile) -> Unit, onBack: () -> Unit) {
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
    var caller by remember { mutableStateOf("Mom") }
    var delayLabel by remember { mutableStateOf("Immediate") }
    var incoming by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(false) }
    var scheduledDelaySeconds by remember { mutableIntStateOf(0) }
    val callers = listOf("Mom", "Brother", "Friend", "Emergency Contact")
    val delays = listOf("Immediate", "10 sec", "30 sec", "1 min")

    LaunchedEffect(scheduledDelaySeconds) {
        if (scheduledDelaySeconds > 0) {
            delay(scheduledDelaySeconds * 1_000L)
            scheduledDelaySeconds = 0
            incoming = true
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
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        KavalPrimaryButton("Answer", { answered = true }, emergency = false)
                        KavalPrimaryButton("Decline", { incoming = false; answered = false }, emergency = true)
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
                        text = if (scheduledDelaySeconds > 0) "Fake Call Scheduled" else "Start Fake Call",
                        onClick = {
                            scheduledDelaySeconds = when (delayLabel) {
                                "10 sec" -> 10
                                "30 sec" -> 30
                                "1 min" -> 60
                                else -> 0
                            }
                            if (scheduledDelaySeconds == 0) incoming = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (scheduledDelaySeconds > 0) {
                    item {
                        KavalGlassCard {
                            Text("Incoming fake call will appear in $scheduledDelaySeconds seconds.")
                            KavalSecondaryButton("Cancel Scheduled Call", { scheduledDelaySeconds = 0 }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
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

    KavalScreen {
        item {
            KavalGlassCard {
                KavalStatusBadge("Alert Sent", KavalColors.Emergency)
                Text("Location Sharing Active", fontWeight = FontWeight.Bold)
                Text("Trusted Contacts Notified: ${state.contacts.size}")
                Text("Emergency ID: ${state.alerts.firstOrNull()?.id ?: "Pending"}")
            }
        }
        item {
            KavalGlassCard {
                KavalSectionHeader("Simulated alert message")
                Text(state.emergencyMessage)
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
