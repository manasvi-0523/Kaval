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
    val emergencyMessage: String = ""
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as KavalApplication).repository

    val uiState: StateFlow<KavalUiState> = combine(
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
