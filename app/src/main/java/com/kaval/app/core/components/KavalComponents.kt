package com.kaval.app.core.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kaval.app.core.theme.KavalColors
import com.kaval.app.domain.model.EmergencyAlert
import com.kaval.app.domain.model.TrustedContact
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun KavalGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun KavalSectionHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KavalColors.Muted)
    }
}

@Composable
fun KavalStatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun KavalPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, emergency: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emergency) KavalColors.Emergency else MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KavalSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(50.dp), shape = RoundedCornerShape(14.dp)) {
        Text(text)
    }
}

@Composable
fun KavalSOSButton(onActivate: () -> Unit, modifier: Modifier = Modifier) {
    val isPressed = remember { mutableStateOf(false) }
    val hasActivated = remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "sos-pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    LaunchedEffect(isPressed.value) {
        if (isPressed.value) {
            delay(2_000)
            if (isPressed.value && !hasActivated.value) {
                hasActivated.value = true
                isPressed.value = false
                onActivate()
            }
        } else {
            hasActivated.value = false
        }
    }

    Box(modifier = modifier.size(224.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(196.dp)
                .scale(pulse)
                .background(KavalColors.Emergency.copy(alpha = 0.14f), CircleShape)
        )
        Box(
            Modifier
                .size(176.dp)
                .scale(if (isPressed.value) 0.96f else 1f)
                .background(KavalColors.Emergency, CircleShape)
                .border(8.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed.value = true
                        waitForUpOrCancellation()
                        if (!hasActivated.value) isPressed.value = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SOS", color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                Text("Hold for emergency", color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

data class KavalNavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun KavalBottomNavBar(currentRoute: String?, items: List<KavalNavItem>, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
fun KavalContactCard(contact: TrustedContact, onEdit: () -> Unit, onDelete: () -> Unit) {
    KavalGlassCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContactPhone, contentDescription = null, tint = KavalColors.Trust)
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(contact.name, fontWeight = FontWeight.Bold)
                    if (contact.isPrimary) KavalStatusBadge("Primary", KavalColors.Safe)
                }
                Text(contact.relationship, color = KavalColors.Muted)
                Text(contact.phoneNumber)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KavalSecondaryButton("Edit", onEdit, Modifier.weight(1f))
            KavalSecondaryButton("Delete", onDelete, Modifier.weight(1f))
        }
    }
}

@Composable
fun KavalActivityCard(alert: EmergencyAlert) {
    KavalGlassCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = KavalColors.Emergency)
            Column(Modifier.weight(1f)) {
                Text(alert.type, fontWeight = FontWeight.Bold)
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(alert.timestamp)), color = KavalColors.Muted)
            }
            if (alert.isDemo) KavalStatusBadge("Demo", KavalColors.Trust)
        }
        Text("Status: ${alert.status}")
        Text("Location: ${alert.locationLabel}")
        Text("Contacts Notified: ${alert.contactsNotified}")
    }
}

@Composable
fun KavalRiskCard(title: String, description: String, color: Color, icon: ImageVector = Icons.Default.CheckCircle) {
    KavalGlassCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, color = KavalColors.Muted)
            }
        }
    }
}

val BottomNavItems = listOf(
    KavalNavItem("home", "Home", Icons.Default.Home),
    KavalNavItem("map", "Map", Icons.Default.Map),
    KavalNavItem("contacts", "Contacts", Icons.Default.ContactPhone),
    KavalNavItem("activity", "Activity", Icons.Default.Warning)
)

@Composable
fun EmptyState(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        textAlign = TextAlign.Center,
        color = KavalColors.Muted
    )
}

@Composable
fun QuickAction(label: String, icon: ImageVector = Icons.Default.Call, modifier: Modifier = Modifier, onClick: () -> Unit) {
    KavalGlassCard(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick)
    ) {
        Spacer(Modifier.height(0.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}
