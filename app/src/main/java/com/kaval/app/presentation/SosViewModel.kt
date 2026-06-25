package com.kaval.app.presentation

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kaval.app.KavalApplication
import com.kaval.app.data.sms.SmsDeliveryStatusReceiver
import com.kaval.app.domain.model.EmergencyAlert
import com.kaval.app.domain.model.KavalLocation
import com.kaval.app.domain.model.LocationStatus
import com.kaval.app.domain.model.LocationPermissionLevel
import com.kaval.app.domain.model.TrustedContact
import com.kaval.app.service.KavalForegroundService
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SosViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as KavalApplication
    private val repository = app.repository
    private val locationTracker = app.locationTracker
    private val trackingClient = app.trackingClient

    fun trigger(
        state: KavalUiState,
        smsPermissionGranted: Boolean,
        audioPermissionGranted: Boolean,
        onEmergencyReady: () -> Unit
    ) = viewModelScope.launch {
        onEmergencyReady()
        val latestLocationState = locationTracker.state.value
        val location = latestLocationState.location
        val contacts = state.contacts
            .mapNotNull { contact -> normalizePhoneNumber(contact.phoneNumber)?.let { contact to it } }
        val permissionStatus = if (state.demoMode) "not_required" else if (smsPermissionGranted) "granted" else "denied"
        val initialSmsStatus = when {
            state.demoMode -> "demo_blocked"
            !smsPermissionGranted -> "permission_denied"
            contacts.isEmpty() -> "no_valid_sms_contacts"
            else -> "queued"
        }
        val errorReason = when {
            !state.demoMode && !smsPermissionGranted -> "SMS permission denied"
            !state.demoMode && contacts.isEmpty() -> "No valid SMS-ready trusted contacts"
            else -> null
        }
        val locationAttached = location != null && latestLocationState.status != LocationStatus.PERMISSION_NEEDED
        val trackingToken = if (
            !state.demoMode &&
            trackingClient.isBackendConfigured &&
            trackingClient.isGuardianWebConfigured &&
            state.locationState.permissionLevel != LocationPermissionLevel.NONE
        ) {
            trackingClient.newTrackingToken()
        } else {
            null
        }
        val trackingUrl = trackingToken?.let(trackingClient::trackingUrl)
        val message = buildSosMessage(state, location, trackingUrl)
        val incidentId = repository.saveAlert(
            EmergencyAlert(
                type = MESSAGE_TYPE,
                title = "SOS Alert",
                message = message,
                timestamp = System.currentTimeMillis(),
                status = "Active",
                locationLabel = if (locationAttached) "Location attached" else "Location unavailable",
                contactsNotified = 0,
                isDemo = state.demoMode,
                locationStatus = if (locationAttached) "attached" else "unavailable",
                mapsLink = location?.mapsLink,
                smsStatus = initialSmsStatus,
                contactsAttempted = if (state.demoMode) 0 else contacts.size,
                permissionStatus = permissionStatus,
                errorReason = errorReason
            )
        )

        coroutineScope {
            launch {
                if (!state.demoMode && (audioPermissionGranted || trackingToken != null)) {
                    runCatching {
                        KavalForegroundService.start(
                            context = app,
                            incidentId = incidentId,
                            trackingToken = trackingToken,
                            displayName = state.profile.name,
                            recordAudio = audioPermissionGranted
                        )
                    }
                }
            }
            launch {
                if (!state.demoMode && smsPermissionGranted && contacts.isNotEmpty()) {
                    sendMultipartSms(incidentId, contacts, message, MESSAGE_TYPE)
                }
            }
            launch {
                if (!state.demoMode) {
                    locationTracker.refreshLocation()
                    val resolved = locationTracker.state.value.location
                    if (location == null && resolved != null && smsPermissionGranted && contacts.isNotEmpty()) {
                        sendGuardianUpdate(
                            state,
                            "LOCATION_UPDATE",
                            "Updated emergency location is now available."
                        )
                    }
                }
            }
        }
    }

    fun sendGuardianUpdate(
        state: KavalUiState,
        messageType: String,
        update: String
    ) = viewModelScope.launch {
        val contacts = state.contacts
            .mapNotNull { contact -> normalizePhoneNumber(contact.phoneNumber)?.let { contact to it } }
        if (state.demoMode || contacts.isEmpty()) return@launch

        locationTracker.refreshLocation()
        val location = locationTracker.state.value.location
        val message = buildList {
            add("Kaval emergency update: $update")
            add("User: ${state.profile.name.ifBlank { "Kaval User" }}")
            add("Location: ${location?.mapsLink ?: "unavailable"}")
            trackingClient.activeTrackingToken()
                ?.let(trackingClient::trackingUrl)
                ?.let { add("Live: $it") }
        }.joinToString("\n")
        val incidentId = repository.saveAlert(
            EmergencyAlert(
                type = messageType,
                title = messageType.replace('_', ' '),
                message = message,
                timestamp = System.currentTimeMillis(),
                status = "Active",
                locationLabel = if (location != null) "Location attached" else "Location unavailable",
                contactsNotified = 0,
                isDemo = false,
                locationStatus = if (location != null) "attached" else "unavailable",
                mapsLink = location?.mapsLink,
                smsStatus = "queued",
                contactsAttempted = contacts.size,
                permissionStatus = "granted"
            )
        )
        sendMultipartSms(incidentId, contacts, message, messageType)
    }

    fun completeTrackingSession() = viewModelScope.launch {
        trackingClient.completeActiveSession()
    }

    private suspend fun sendMultipartSms(
        incidentId: Long,
        contacts: List<Pair<TrustedContact, String>>,
        message: String,
        messageType: String
    ) {
        val subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId()
        @Suppress("DEPRECATION")
        val smsManager = if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else {
            SmsManager.getDefault()
        }
        val parts = smsManager.divideMessage(message)
        repository.initializeSmsDeliveries(
            incidentId = incidentId,
            contacts = contacts,
            messageType = messageType,
            subscriptionId = subscriptionId,
            partCount = parts.size
        )

        contacts.forEach { (contact, normalizedPhone) ->
            try {
                val sentIntents = ArrayList<PendingIntent>(parts.size)
                val deliveredIntents = ArrayList<PendingIntent>(parts.size)
                parts.indices.forEach { partIndex ->
                    sentIntents += statusPendingIntent(
                        action = SmsDeliveryStatusReceiver.ACTION_SMS_SENT,
                        incidentId = incidentId,
                        contactId = contact.id,
                        messageType = messageType,
                        partIndex = partIndex,
                        partCount = parts.size
                    )
                    deliveredIntents += statusPendingIntent(
                        action = SmsDeliveryStatusReceiver.ACTION_SMS_DELIVERED,
                        incidentId = incidentId,
                        contactId = contact.id,
                        messageType = messageType,
                        partIndex = partIndex,
                        partCount = parts.size
                    )
                }
                smsManager.sendMultipartTextMessage(
                    normalizedPhone,
                    null,
                    parts,
                    sentIntents,
                    deliveredIntents
                )
            } catch (error: SecurityException) {
                repository.updateSmsSent(
                    incidentId, contact.id, messageType, "FAILED",
                    error.message ?: "SMS permission denied for subscription $subscriptionId", SmsManager.RESULT_ERROR_GENERIC_FAILURE
                )
            } catch (error: RuntimeException) {
                repository.updateSmsSent(
                    incidentId, contact.id, messageType, "FAILED",
                    error.message ?: "SMS send failed for subscription $subscriptionId", SmsManager.RESULT_ERROR_GENERIC_FAILURE
                )
            }
        }
    }

    private fun statusPendingIntent(
        action: String,
        incidentId: Long,
        contactId: Long,
        messageType: String,
        partIndex: Int,
        partCount: Int
    ): PendingIntent {
        val intent = Intent(app, SmsDeliveryStatusReceiver::class.java).apply {
            this.action = action
            putExtra(SmsDeliveryStatusReceiver.EXTRA_INCIDENT_ID, incidentId)
            putExtra(SmsDeliveryStatusReceiver.EXTRA_CONTACT_ID, contactId)
            putExtra(SmsDeliveryStatusReceiver.EXTRA_MESSAGE_TYPE, messageType)
            putExtra(SmsDeliveryStatusReceiver.EXTRA_FINAL_PART, partIndex == partCount - 1)
        }
        val requestCode = listOf(action, incidentId, contactId, partIndex).hashCode()
        return PendingIntent.getBroadcast(
            app,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildSosMessage(
        state: KavalUiState,
        location: KavalLocation?,
        trackingUrl: String?
    ): String {
        val profileName = state.profile.name.trim().ifBlank { "Kaval User" }
        val customMessage = state.profile.emergencyNote
            .lines()
            .joinToString(" ") { it.trim() }
            .replace(Regex(" +"), " ")
            .trim()
            .ifBlank { "I may be in danger. Please check on me immediately." }
            .take(MAX_CUSTOM_MESSAGE_LENGTH)
        val time = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date())

        return buildList {
            add("SOS: $profileName needs help.")
            add(customMessage)
            if (location != null) {
                add("Location: ${location.mapsLink}")
            } else {
                add("Location unavailable - GPS off or no signal")
            }
            trackingUrl?.let { add("Live tracking: $it") }
            add("Time: $time")
            add("Sent via Kaval")
        }.joinToString("\n")
    }

    private fun normalizePhoneNumber(raw: String): String? {
        val trimmed = raw.trim()
        val digits = trimmed.filter(Char::isDigit)
        return when {
            trimmed.startsWith("+") && digits.length in 8..15 -> "+$digits"
            digits.length == 10 -> "+91$digits"
            digits.length == 11 && digits.startsWith("0") -> "+91${digits.drop(1)}"
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            digits.length in 8..15 -> "+$digits"
            else -> null
        }
    }

    companion object {
        private const val MESSAGE_TYPE = "SOS"
        private const val MAX_CUSTOM_MESSAGE_LENGTH = 120
    }
}
