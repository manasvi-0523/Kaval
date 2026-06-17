package com.kaval.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kaval.app.KavalApplication
import com.kaval.app.domain.model.AppearanceSettings
import com.kaval.app.domain.model.EmergencyAlert
import com.kaval.app.domain.model.SafetyStatus
import com.kaval.app.domain.model.TrustedContact
import com.kaval.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KavalUiState(
    val contacts: List<TrustedContact> = emptyList(),
    val alerts: List<EmergencyAlert> = emptyList(),
    val demoMode: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val safetyStatus: SafetyStatus = SafetyStatus(),
    val emergencyMessage: String = "",
    val guardianModeActive: Boolean = false,
    val passiveSafetyActive: Boolean = false,
    val journeyActive: Boolean = false,
    val journeyPhase: String = "Before",
    val journeyStatus: String = "No journey active"
)

private data class SafetyModes(
    val guardianModeActive: Boolean = false,
    val passiveSafetyActive: Boolean = false,
    val journeyActive: Boolean = false,
    val journeyPhase: String = "Before",
    val journeyStatus: String = "No journey active"
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as KavalApplication).repository
    private val safetyModes = MutableStateFlow(SafetyModes())

    private val persistedState = combine(
        repository.contacts,
        repository.alerts,
        repository.demoMode,
        repository.profile,
        repository.appearance
    ) { contacts, alerts, demoMode, profile, appearance ->
        val locationSharing = alerts.firstOrNull()?.status == "Active"
        KavalUiState(
            contacts = contacts,
            alerts = alerts,
            demoMode = demoMode,
            profile = profile,
            appearance = appearance,
            safetyStatus = SafetyStatus(
                status = if (locationSharing) "Emergency mode active" else "You are currently safe",
                locationSharingActive = locationSharing,
                riskLevel = if (locationSharing) "Tracking Active" else "Safe Zone"
            ),
            emergencyMessage = buildEmergencyMessage(profile)
        )
    }

    val uiState: StateFlow<KavalUiState> = combine(
        persistedState,
        safetyModes
    ) { state, modes ->
        state.copy(
            guardianModeActive = modes.guardianModeActive,
            passiveSafetyActive = modes.passiveSafetyActive,
            journeyActive = modes.journeyActive,
            journeyPhase = modes.journeyPhase,
            journeyStatus = modes.journeyStatus
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KavalUiState())

    fun saveContact(contact: TrustedContact) = viewModelScope.launch {
        repository.saveContact(contact)
    }

    fun deleteContact(contact: TrustedContact) = viewModelScope.launch {
        repository.deleteContact(contact)
    }

    fun triggerEmergency() = viewModelScope.launch {
        val state = uiState.value
        repository.saveAlert(
            EmergencyAlert(
                type = "SOS Alert",
                timestamp = System.currentTimeMillis(),
                status = "Active",
                locationLabel = "Demo Location",
                contactsNotified = state.contacts.size,
                isDemo = state.demoMode
            )
        )
    }

    fun stopEmergency() = viewModelScope.launch {
        val state = uiState.value
        repository.saveAlert(
            EmergencyAlert(
                type = "Emergency Mode",
                timestamp = System.currentTimeMillis(),
                status = "Completed",
                locationLabel = "Demo Location",
                contactsNotified = state.contacts.size,
                isDemo = state.demoMode
            )
        )
    }

    fun setDemoMode(enabled: Boolean) = viewModelScope.launch {
        repository.setDemoMode(enabled)
    }

    fun saveProfile(profile: UserProfile) = viewModelScope.launch {
        repository.saveProfile(profile)
    }

    fun saveAppearance(settings: AppearanceSettings) = viewModelScope.launch {
        repository.saveAppearance(settings)
    }

    fun setGuardianMode(enabled: Boolean) {
        val current = safetyModes.value
        safetyModes.value = current.copy(
            guardianModeActive = enabled,
            passiveSafetyActive = if (enabled) true else current.passiveSafetyActive,
            journeyStatus = if (enabled) "Guardian is watching your route" else if (current.journeyActive) current.journeyStatus else "No journey active"
        )
    }

    fun setPassiveSafety(enabled: Boolean) {
        val current = safetyModes.value
        safetyModes.value = current.copy(
            passiveSafetyActive = enabled,
            journeyStatus = if (enabled && !current.journeyActive) {
                "Passive monitoring active"
            } else if (!enabled && !current.journeyActive) {
                "No journey active"
            } else {
                current.journeyStatus
            }
        )
    }

    fun startJourney() {
        safetyModes.value = safetyModes.value.copy(
            journeyActive = true,
            journeyPhase = "During",
            journeyStatus = "Journey active - ETA 18 min",
            guardianModeActive = true,
            passiveSafetyActive = true
        )
    }

    fun markBoarded() {
        safetyModes.value = safetyModes.value.copy(
            journeyActive = true,
            journeyPhase = "During",
            journeyStatus = "I've boarded - guardian update queued",
            guardianModeActive = true
        )
    }

    fun markReached() {
        safetyModes.value = safetyModes.value.copy(
            journeyActive = false,
            journeyPhase = "After",
            journeyStatus = "Arrived safely - guardian anxiety cleared",
            guardianModeActive = false
        )
    }
}

private fun buildEmergencyMessage(profile: UserProfile): String {
    return """
        Emergency Alert from ${profile.name}.

        ${profile.emergencyNote}

        Location:
        Demo Location

        Time:
        Current device time

        Battery:
        82%

        Sent via Kaval.
    """.trimIndent()
}
