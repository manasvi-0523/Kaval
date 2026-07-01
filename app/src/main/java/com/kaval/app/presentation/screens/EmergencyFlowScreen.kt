package com.kaval.app.presentation.screens

import android.app.Activity
import android.content.Intent
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaval.app.domain.model.EmergencyFlowStage
import com.kaval.app.domain.model.EscapeAbility
import com.kaval.app.domain.model.SituationClass
import com.kaval.app.domain.model.SituationAnalysis
import com.kaval.app.domain.model.SituationLocation
import com.kaval.app.domain.model.SituationType
import com.kaval.app.domain.model.SosAnswers
import com.kaval.app.domain.model.SpeakingAbility
import com.kaval.app.presentation.EmergencyFlowViewModel
import com.kaval.app.presentation.KavalUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EmergencyBackground = Color(0xFF0B0F0E)
private val EmergencyRed = Color(0xFFD95D5D)
private val EmergencyGreen = Color(0xFF1D7A66)
private val EmergencyAmber = Color(0xFFD9823B)
private val EmergencySurface = Color(0xFF151A18)
private val EmergencySurfaceRaised = Color(0xFF202622)
private val EmergencyMuted = Color(0xFFA8B0AA)

private data class QuestionOption<T>(val label: String, val value: T)

@Composable
private fun EmergencyOrientationLock() {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        val previousOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity.requestedOrientation = previousOrientation
        }
    }
}

@Composable
fun EmergencyFlowScreen(
    state: KavalUiState,
    onGuardianUpdate: (String, String) -> Unit,
    onSafe: () -> Unit,
    onStopRecording: () -> Unit,
    flowViewModel: EmergencyFlowViewModel = viewModel()
) {
    BackHandler(enabled = true) {}
    val context = LocalContext.current
    val alert = state.alerts.firstOrNull()
    val flow by flowViewModel.state.collectAsStateWithLifecycle()
    var alarm by remember { mutableStateOf<Ringtone?>(null) }
    EmergencyOrientationLock()

    DisposableEffect(Unit) {
        onDispose { alarm?.stop() }
    }

    LaunchedEffect(flow.noResponseEscalationPending) {
        if (flow.noResponseEscalationPending) {
            onGuardianUpdate(
                "NO_RESPONSE_ESCALATED",
                "No response after SOS. Call immediately and consider emergency services."
            )
            flowViewModel.markNoResponseEscalationShared()
        }
    }

    LaunchedEffect(flow.guardianAnalysisRevision, flow.guardianAnalysisSharedRevision) {
        val analysis = flow.analysis
        if (analysis != null &&
            flow.guardianAnalysisRevision > flow.guardianAnalysisSharedRevision
        ) {
            onGuardianUpdate(
                "SITUATION_CLASS_${analysis.situationClass.name}",
                "Kaval analysis: ${analysis.title}. ${analysis.guardianBrief} " +
                    "User responses: ${analysis.answerSummary}."
            )
            flowViewModel.markGuardianAnalysisShared(flow.guardianAnalysisRevision)
        }
    }

    if (flow.stealthActive) {
        StealthCallOverlay(
            onReveal = { flowViewModel.setStealth(false) },
            onEnd = { flowViewModel.setStealth(false) }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmergencyBackground)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        when (flow.stage) {
            EmergencyFlowStage.RESPONSE_CHECK -> ResponseCheck(
                state = state,
                seconds = flow.responseSeconds,
                onRespond = flowViewModel::userResponded,
                onCannotRespond = {
                    flowViewModel.cannotRespond()
                    onGuardianUpdate("NO_RESPONSE_ESCALATED", "User confirmed they cannot respond. Call immediately.")
                },
                onSafe = { flowViewModel.setSafeConfirmationVisible(true) },
                onCall112 = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
                onStopRecording = onStopRecording,
                onMuteCard = { flowViewModel.setHelpCardVisible(true) }
            )

            EmergencyFlowStage.CRITICAL -> CriticalActions(
                onSafe = { flowViewModel.setSafeConfirmationVisible(true) },
                onStillInDanger = {
                    onGuardianUpdate("STILL_IN_DANGER", "Confirmed still in danger. Monitor location and call immediately.")
                },
                onCall112 = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
                onStopRecording = onStopRecording
            )

            EmergencyFlowStage.QUESTIONS -> QuestionFlow(
                questionIndex = flow.questionIndex,
                seconds = flow.questionSeconds,
                answers = flow.answers,
                onAnswer = flowViewModel::submitAnswer
            )

            EmergencyFlowStage.SUGGESTIONS -> SuggestionScreen(
                analysis = flow.analysis,
                state = state,
                onStealth = { flowViewModel.setStealth(true) },
                onSafe = { flowViewModel.setSafeConfirmationVisible(true) },
                onGuardianUpdate = onGuardianUpdate,
                onCall = { number ->
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                },
                onEscalateToD = {
                    flowViewModel.escalateToClassD()
                    onGuardianUpdate("DANGER_ESCALATED", "Situation worsened. Police help may be needed immediately.")
                },
                onAlarm = {
                    val audioManager = context.getSystemService(AudioManager::class.java)
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_ALARM,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                        0
                    )
                    alarm?.stop()
                    alarm = RingtoneManager.getRingtone(
                        context,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ).also { it.play() }
                },
                onSilent = {
                    alarm?.stop()
                    alarm = null
                },
                onMuteCard = { flowViewModel.setHelpCardVisible(true) },
                onRestartAssessment = flowViewModel::restartAssessment
            )
        }

        if (flow.helpCardVisible) {
            MuteHelpCard(
                state = state,
                onDismiss = { flowViewModel.setHelpCardVisible(false) }
            )
        }
        if (flow.safeConfirmationVisible) {
            AlertDialog(
                onDismissRequest = { flowViewModel.setSafeConfirmationVisible(false) },
                title = { Text("Confirm you are safe") },
                text = { Text("This stops emergency monitoring and sends an \"I am safe\" update to your trusted contacts.") },
                confirmButton = {
                    TextButton(onClick = {
                        flowViewModel.setSafeConfirmationVisible(false)
                        onSafe()
                    }) { Text("I AM SAFE") }
                },
                dismissButton = {
                    TextButton(onClick = { flowViewModel.setSafeConfirmationVisible(false) }) {
                        Text("KEEP EMERGENCY ACTIVE")
                    }
                }
            )
        }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun ResponseCheck(
    state: KavalUiState,
    seconds: Int,
    onRespond: () -> Unit,
    onCannotRespond: () -> Unit,
    onSafe: () -> Unit,
    onCall112: () -> Unit,
    onStopRecording: () -> Unit,
    onMuteCard: () -> Unit
) {
    val alert = state.alerts.firstOrNull()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("EMERGENCY ACTIVE", color = EmergencyRed, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Since ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(alert?.timestamp ?: System.currentTimeMillis()))}",
                color = EmergencyMuted,
                fontSize = 12.sp
            )
        }
        item {
            StatusPanel(state)
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Can you respond?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        item { EmergencyButton("YES - I'M HERE", EmergencyGreen, onRespond, 64) }
        item { EmergencyButton("NO / CAN'T RESPOND", EmergencyRed, onCannotRespond, 64) }
        item {
            LinearProgressIndicator(
                progress = { (15 - seconds).coerceIn(0, 15) / 15f },
                modifier = Modifier.fillMaxWidth(),
                color = EmergencyRed,
                trackColor = EmergencySurface
            )
            Text("Auto-classifying in: ${seconds.coerceAtLeast(0)}s", color = EmergencyMuted, fontSize = 12.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmergencyButton("I AM SAFE", EmergencyGreen, onSafe, 52, Modifier.weight(1f))
                EmergencyButton("CALL 112", EmergencyRed, onCall112, 52, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmergencyGhostButton("HELP CARD", onMuteCard, Modifier.weight(1f))
                EmergencyGhostButton("STOP RECORDING", onStopRecording, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusPanel(state: KavalUiState) {
    val alert = state.alerts.firstOrNull()
    Surface(color = EmergencySurface, shape = RoundedCornerShape(6.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Emergency systems", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            EmergencyStatusRow(
                label = "SMS",
                value = "${alert?.sentCount ?: 0} sent / ${alert?.failedCount ?: 0} failed"
            )
            EmergencyStatusRow(
                label = "Live tracking",
                value = trackingUploadLabel(state)
            )
            EmergencyStatusRow(
                label = "Location",
                value = alert?.locationLabel ?: "Checking GPS"
            )
            EmergencyStatusRow(
                label = "Audio",
                value = if (alert?.audioFilePath != null) "Recording locally" else "Skipped or starting"
            )
        }
    }
}

@Composable
private fun EmergencyStatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = EmergencyMuted, fontSize = 12.sp, modifier = Modifier.weight(0.42f))
        Text(
            value,
            color = Color.White,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun CriticalActions(
    onSafe: () -> Unit,
    onStillInDanger: () -> Unit,
    onCall112: () -> Unit,
    onStopRecording: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "CRITICAL - UNABLE TO RESPOND",
            color = EmergencyRed,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 22.dp)
        )
        EmergencyButton("I AM SAFE", Color(0xFF1B5E20), onSafe, 80)
        Spacer(Modifier.height(12.dp))
        EmergencyButton("STILL IN DANGER", EmergencyAmber, onStillInDanger, 80)
        Spacer(Modifier.height(12.dp))
        EmergencyButton("CALL 112", Color(0xFFB71C1C), onCall112, 80)
        Spacer(Modifier.height(12.dp))
        EmergencyGhostButton("STOP RECORDING", onStopRecording, Modifier.height(80.dp))
    }
}

@Composable
private fun QuestionFlow(
    questionIndex: Int,
    seconds: Int,
    answers: SosAnswers,
    onAnswer: (SosAnswers) -> Unit
) {
    val title: String
    val options: List<QuestionOption<*>>
    when (questionIndex) {
        0 -> {
            title = "Where are you?"
            options = listOf(
                QuestionOption("In a cab / auto / vehicle", SituationLocation.IN_VEHICLE),
                QuestionOption("Walking outside", SituationLocation.WALKING),
                QuestionOption("In a public place", SituationLocation.PUBLIC_PLACE),
                QuestionOption("At home / hostel / PG", SituationLocation.HOME),
                QuestionOption("With someone unsafe", SituationLocation.WITH_UNSAFE_PERSON),
                QuestionOption("Other / I don't know", SituationLocation.UNKNOWN)
            )
        }
        1 -> {
            title = "What is happening?"
            options = listOf(
                QuestionOption("Being followed", SituationType.BEING_FOLLOWED),
                QuestionOption("Driver taking wrong route", SituationType.WRONG_ROUTE),
                QuestionOption("Harassment / uncomfortable person", SituationType.HARASSMENT),
                QuestionOption("Threat / violence risk", SituationType.THREAT_VIOLENCE),
                QuestionOption("Lost / stranded", SituationType.STRANDED),
                QuestionOption("Medical issue", SituationType.MEDICAL),
                QuestionOption("I just feel unsafe", SituationType.JUST_UNSAFE)
            )
        }
        2 -> {
            title = "Can you speak?"
            options = listOf(
                QuestionOption("Yes, I can talk freely", SpeakingAbility.CAN_SPEAK),
                QuestionOption("I must pretend everything is normal", SpeakingAbility.PRETEND_NORMAL),
                QuestionOption("No, I cannot speak at all", SpeakingAbility.CANNOT_SPEAK),
                QuestionOption("I can only tap", SpeakingAbility.CAN_ONLY_TAP)
            )
        }
        else -> {
            title = "Can you escape now?"
            options = listOf(
                QuestionOption("Yes, I can move to safety", EscapeAbility.CAN_ESCAPE),
                QuestionOption("Maybe, I need help first", EscapeAbility.NEED_HELP),
                QuestionOption("No, I am trapped", EscapeAbility.TRAPPED),
                QuestionOption("I don't know", EscapeAbility.DONT_KNOW)
            )
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("SITUATION CHECK  ${questionIndex + 1}/4", color = EmergencyRed, fontSize = 13.sp)
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Auto-skip in ${seconds}s", color = EmergencyMuted, fontSize = 12.sp)
        }
        items(options) { option ->
            EmergencyGhostButton(option.label, {
                val updated = when (val value = option.value) {
                    is SituationLocation -> answers.copy(location = value)
                    is SituationType -> answers.copy(situation = value)
                    is SpeakingAbility -> answers.copy(canSpeak = value)
                    is EscapeAbility -> answers.copy(canEscape = value)
                    else -> answers
                }
                onAnswer(updated)
            })
        }
    }
}

@Composable
private fun SuggestionScreen(
    analysis: SituationAnalysis?,
    state: KavalUiState,
    onStealth: () -> Unit,
    onSafe: () -> Unit,
    onGuardianUpdate: (String, String) -> Unit,
    onCall: (String) -> Unit,
    onEscalateToD: () -> Unit,
    onAlarm: () -> Unit,
    onSilent: () -> Unit,
    onMuteCard: () -> Unit,
    onRestartAssessment: () -> Unit
) {
    val situationClass = analysis?.situationClass ?: SituationClass.C
    if (situationClass == SituationClass.D) {
        ClassDActions(
            analysis = analysis,
            guardianStatus = guardianAnalysisStatus(state),
            onGuardianUpdate = onGuardianUpdate,
            onAlarm = onAlarm,
            onSilent = onSilent,
            onStealth = onStealth,
            onRestartAssessment = onRestartAssessment
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SITUATION IDENTIFIED", color = EmergencyRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        analysis?.title ?: "Unsafe situation",
                        color = Color.White,
                        fontSize = 27.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Response class ${situationClass.name}",
                        color = EmergencyMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
        item {
            Surface(color = EmergencySurface, shape = RoundedCornerShape(6.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Keep calm", color = EmergencyMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        analysis?.calmPrompt ?: "Breathe slowly. Keep location sharing active.",
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(color = Color(0xFF242424), shape = RoundedCornerShape(6.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("First priority", color = EmergencyRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                analysis?.primaryAction ?: "Move toward a safer public place.",
                                color = Color.White,
                                fontSize = 15.sp,
                                lineHeight = 21.sp
                            )
                        }
                    }
                    Text("Do this now", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    analysis?.userAdvice.orEmpty().forEachIndexed { index, advice ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${index + 1}", color = EmergencyRed, fontWeight = FontWeight.Bold)
                            Text(advice, color = Color.White, modifier = Modifier.weight(1f), lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
        item {
            Surface(color = Color(0xFF211817), shape = RoundedCornerShape(6.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Avoid", color = Color(0xFFFFA28D), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    analysis?.avoidAdvice.orEmpty().forEach { warning ->
                        Text("- $warning", color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
        item {
            Surface(color = Color(0xFF10231F), shape = RoundedCornerShape(6.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        guardianAnalysisStatus(state),
                        color = Color(0xFF78D7BE),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        analysis?.guardianBrief ?: "Guardian was told to call and monitor your location.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
        when (situationClass) {
            SituationClass.A -> {
                item { EmergencyButton("CALL GUARDIAN NOW", EmergencyGreen, { state.contacts.firstOrNull()?.let { onCall(it.phoneNumber) } }) }
                item {
                    EmergencyGhostButton(
                        "SEND LATEST LOCATION",
                        { onGuardianUpdate("LOCATION_RESHARED", "Latest verified emergency location shared again.") }
                    )
                }
                item { EmergencyGhostButton("GUARDIAN CODED SCRIPTS", { onGuardianUpdate("CODED_SCRIPT", "Please keep checking the map.") }) }
                item { EmergencyGhostButton("SHOW HELP CARD", onMuteCard) }
            }
            SituationClass.B -> {
                item { EmergencyButton("CALL GUARDIAN", EmergencyGreen, { state.contacts.firstOrNull()?.let { onCall(it.phoneNumber) } }) }
                item { EmergencyGhostButton("SHARE LOCATION AGAIN", { onGuardianUpdate("LOCATION_RESHARED", "Emergency location shared again.") }) }
                item { EmergencyGhostButton("SHOW HELP CARD", onMuteCard) }
            }
            SituationClass.C -> {
                item { EmergencyGhostButton("GET EXIT EXCUSE", { onGuardianUpdate("EXIT_CALL_REQUEST", "Please call me now and help me leave this situation.") }) }
                item { EmergencyGhostButton("5-MINUTE CHECK-IN", { onGuardianUpdate("CHECK_IN_STARTED", "A 5-minute safety check-in has started.") }) }
                item { EmergencyButton("TAP IF IT WORSENS", EmergencyRed, onEscalateToD) }
            }
            SituationClass.E -> {
                item { EmergencyButton("CALL 108 - AMBULANCE", EmergencyRed, { onCall("108") }) }
                item { EmergencyButton("CALL 112 - ALL EMERGENCY", EmergencyRed, { onCall("112") }) }
                item { EmergencyGhostButton("SHOW EMERGENCY NOTE", onMuteCard) }
                item {
                    state.profile.medicalNote?.takeIf { it.isNotBlank() }?.let {
                        Surface(color = Color.White, shape = RoundedCornerShape(6.dp)) {
                            Text(it, color = Color.Black, fontSize = 18.sp, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
            else -> Unit
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmergencyButton("CALL 112", EmergencyRed, { onCall("112") }, 52, Modifier.weight(1f))
                EmergencyButton("I AM SAFE", EmergencyGreen, onSafe, 52, Modifier.weight(1f))
            }
        }
        item {
            TextButton(onClick = onRestartAssessment, modifier = Modifier.fillMaxWidth()) {
                Text("Review or change situation", color = EmergencyMuted)
            }
        }
    }
}

@Composable
private fun ClassDActions(
    analysis: SituationAnalysis?,
    guardianStatus: String,
    onGuardianUpdate: (String, String) -> Unit,
    onAlarm: () -> Unit,
    onSilent: () -> Unit,
    onStealth: () -> Unit,
    onRestartAssessment: () -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("TAP-ONLY RESPONSE", color = EmergencyRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    analysis?.title ?: "Trapped or unable to speak",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onStealth) {
                Icon(Icons.Default.Visibility, contentDescription = "Open stealth call screen", tint = Color.White)
            }
        }
        Surface(color = Color(0xFF10231F), shape = RoundedCornerShape(6.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    analysis?.calmPrompt ?: "Stay quiet. Use only tap controls.",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
                Text(
                    "$guardianStatus. ${analysis?.guardianBrief ?: "Avoid repeated calls; monitor location and consider police."}",
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
        Spacer(Modifier.weight(1f))
        EmergencyButton("SEND POLICE HELP REQUEST", Color(0xFFB71C1C), {
            onGuardianUpdate("POLICE_HELP_REQUEST", "Police help needed immediately. I cannot speak safely.")
        }, 80)
        Spacer(Modifier.height(12.dp))
        EmergencyButton("SHARE LOCATION AGAIN", EmergencyAmber, {
            onGuardianUpdate("LOCATION_RESHARED", "Police help requested. Emergency location shared again.")
        }, 80)
        Spacer(Modifier.height(12.dp))
        EmergencyButton("START LOUD ALARM", EmergencyRed, onAlarm, 80)
        Spacer(Modifier.height(12.dp))
        EmergencyGhostButton("KEEP SILENT", onSilent, Modifier.height(80.dp))
        TextButton(onClick = onRestartAssessment, modifier = Modifier.fillMaxWidth()) {
            Text("Review situation", color = Color(0xFF333333))
        }
    }
}

private fun guardianAnalysisStatus(state: KavalUiState): String = when {
    state.demoMode -> "Guardian update blocked by Demo Mode"
    state.contacts.isEmpty() -> "Guardian update not sent: no trusted contact"
    else -> "Guardian analysis queued; check Incident Log for delivery"
}

private fun trackingUploadLabel(state: KavalUiState): String {
    val tracking = state.trackingUploadState
    if (!tracking.active) return "Starting or unavailable"
    if (tracking.hasRecentFailure) {
        return "Retrying upload (${tracking.failureCount})"
    }
    val lastSuccess = tracking.lastSuccessAtMillis ?: return "Starting upload"
    val secondsAgo = ((System.currentTimeMillis() - lastSuccess) / 1_000L).coerceAtLeast(0L)
    return if (secondsAgo < 5) {
        "Active just now"
    } else {
        "Active ${secondsAgo}s ago"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StealthCallOverlay(onReveal: () -> Unit, onEnd: () -> Unit) {
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            elapsed += 1
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF090909)).padding(28.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Emergency Contact", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text("connected", color = EmergencyMuted, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))
            Surface(
                color = Color.Transparent,
                modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onReveal)
            ) {
                Text(
                    "%02d:%02d".format(elapsed / 60, elapsed % 60),
                    color = Color.White,
                    fontSize = 42.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }
            Text("Hold the timer to return", color = Color(0xFF242424), fontSize = 10.sp)
            Spacer(Modifier.height(80.dp))
            Button(
                onClick = onEnd,
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Text("  End call")
            }
        }
    }
}

@Composable
private fun MuteHelpCard(state: KavalUiState, onDismiss: () -> Unit) {
    val primary = state.contacts.firstOrNull { it.isPrimary } ?: state.contacts.firstOrNull()
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "I NEED HELP\n${state.profile.name}\n${state.locationState.location?.mapsLink ?: "LOCATION UNAVAILABLE"}\n" +
                    "GUARDIAN: ${primary?.name ?: "NOT SET"} ${primary?.phoneNumber.orEmpty()}\n" +
                    "I CANNOT SPEAK FREELY\nPLEASE CALL POLICE",
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 29.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            EmergencyGhostButton("CLOSE", onDismiss)
        }
    }
}

@Composable
private fun EmergencyButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    height: Int = 60,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = if (height >= 80) 17.sp else 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmergencyGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}
