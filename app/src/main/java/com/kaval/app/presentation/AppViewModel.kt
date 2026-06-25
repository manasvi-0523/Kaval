package com.kaval.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kaval.app.KavalApplication
import com.kaval.app.domain.model.AppearanceSettings
import com.kaval.app.domain.model.EmergencyAlert
import com.kaval.app.domain.model.KavalLocationState
import com.kaval.app.domain.model.LocationStatus
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
    val audioEvidenceEnabled: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val safetyStatus: SafetyStatus = SafetyStatus(),
    val emergencyMessage: String = "",
    val locationState: KavalLocationState = KavalLocationState(),
    val logRetentionDays: Int = 28,
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
    private val kavalApplication = application as KavalApplication
    private val repository = kavalApplication.repository
    private val locationTracker = kavalApplication.locationTracker
    private val safetyModes = MutableStateFlow(SafetyModes())

    private val appearanceRetentionAndAudio = combine(
        repository.appearance,
        repository.logRetentionDays,
        repository.audioEvidenceEnabled
    ) { appearance, retentionDays, audioEvidenceEnabled ->
        Triple(appearance, retentionDays, audioEvidenceEnabled)
    }

    private val persistedState = combine(
        repository.contacts,
        repository.alerts,
        repository.demoMode,
        repository.profile,
        appearanceRetentionAndAudio
    ) { contacts, alerts, demoMode, profile, settings ->
        val (appearance, retentionDays, audioEvidenceEnabled) = settings
        val locationSharing = alerts.firstOrNull()?.status == "Active"
        KavalUiState(
            contacts = contacts,
            alerts = alerts,
            demoMode = demoMode,
            audioEvidenceEnabled = audioEvidenceEnabled,
            profile = profile,
            appearance = appearance,
            logRetentionDays = retentionDays,
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
        safetyModes,
        locationTracker.state
    ) { state, modes, locationState ->
        state.copy(
            locationState = locationState,
            guardianModeActive = modes.guardianModeActive,
            passiveSafetyActive = modes.passiveSafetyActive,
            journeyActive = modes.journeyActive,
            journeyPhase = modes.journeyPhase,
            journeyStatus = modes.journeyStatus
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KavalUiState())

    init {
        locationTracker.refreshPermissionState()
    }

    fun refreshLocationPermission() {
        locationTracker.refreshPermissionState()
    }

    fun refreshLocation() = viewModelScope.launch {
        locationTracker.refreshLocation()
    }

    fun saveContact(contact: TrustedContact) = viewModelScope.launch {
        repository.saveContact(contact)
    }

    fun deleteContact(contact: TrustedContact) = viewModelScope.launch {
        repository.deleteContact(contact)
    }

    fun stopEmergency() = viewModelScope.launch {
        val state = uiState.value
        val location = state.locationState.location
        repository.saveAlert(
            EmergencyAlert(
                type = "Emergency Mode",
                timestamp = System.currentTimeMillis(),
                status = "Completed",
                locationLabel = if (location != null) "Location attached" else "Location unavailable",
                contactsNotified = state.contacts.size,
                isDemo = state.demoMode,
                locationStatus = if (location != null) "attached" else "unavailable",
                mapsLink = location?.mapsLink
            )
        )
    }

    fun setDemoMode(enabled: Boolean) = viewModelScope.launch {
        repository.setDemoMode(enabled)
    }

    fun setAudioEvidenceEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setAudioEvidenceEnabled(enabled)
    }

    fun setLogRetentionDays(days: Int) = viewModelScope.launch {
        repository.setLogRetentionDays(days)
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
        Latest verified GPS location is attached when available.

        Time:
        Current device time

        Battery:
        82%

        Sent via Kaval.
    """.trimIndent()
}
