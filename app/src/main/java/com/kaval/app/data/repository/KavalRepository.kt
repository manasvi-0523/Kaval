package com.kaval.app.data.repository

import com.kaval.app.data.datastore.KavalPreferences
import com.kaval.app.data.local.dao.IncidentDao
import com.kaval.app.data.local.dao.TrustedContactDao
import com.kaval.app.data.local.entities.SmsDeliveryEntity
import com.kaval.app.data.local.entities.toDomain
import com.kaval.app.data.local.entities.toEntity
import com.kaval.app.domain.model.AppearanceSettings
import com.kaval.app.domain.model.EmergencyAlert
import com.kaval.app.domain.model.TrustedContact
import com.kaval.app.domain.model.UserProfile
import kotlinx.coroutines.flow.map

class KavalRepository(
    private val contactDao: TrustedContactDao,
    private val incidentDao: IncidentDao,
    private val preferences: KavalPreferences
) {
    val contacts = contactDao.observeContacts().map { items -> items.map { it.toDomain() } }
    val alerts = incidentDao.observeIncidents().map { items -> items.map { it.toDomain() } }
    val demoMode = preferences.demoMode
    val logRetentionDays = preferences.logRetentionDays
    val profile = preferences.profile
    val appearance = preferences.appearance

    suspend fun saveContact(contact: TrustedContact) {
        if (contact.id == 0L) {
            val id = contactDao.insert(contact.toEntity())
            if (contact.isPrimary) contactDao.clearOtherPrimary(id)
        } else {
            contactDao.update(contact.toEntity())
            if (contact.isPrimary) contactDao.clearOtherPrimary(contact.id)
        }
    }

    suspend fun deleteContact(contact: TrustedContact) = contactDao.delete(contact.toEntity())

    suspend fun saveAlert(alert: EmergencyAlert): Long = incidentDao.insert(alert.toEntity())

    suspend fun initializeSmsDeliveries(
        incidentId: Long,
        contacts: List<Pair<TrustedContact, String>>,
        messageType: String,
        subscriptionId: Int,
        partCount: Int
    ) {
        incidentDao.insertSmsDeliveries(
            contacts.map { (contact, normalizedPhone) ->
                SmsDeliveryEntity(
                    incidentId = incidentId,
                    contactId = contact.id,
                    contactName = contact.name,
                    phoneNumber = normalizedPhone,
                    messageType = messageType,
                    diagnosticText = "Attempting SMS via subscriptionId=$subscriptionId; parts=$partCount; entered=${contact.phoneNumber}"
                )
            }
        )
    }

    suspend fun updateSmsSent(
        incidentId: Long,
        contactId: Long,
        messageType: String,
        status: String,
        failureReason: String?,
        resultCode: Int
    ) = incidentDao.updateSentAndSummary(
        incidentId, contactId, messageType, status, failureReason, resultCode
    )

    suspend fun updateSmsDelivery(
        incidentId: Long,
        contactId: Long,
        messageType: String,
        status: String,
        failureReason: String?,
        resultCode: Int
    ) = incidentDao.updateDeliveryAndSummary(
        incidentId, contactId, messageType, status, failureReason, resultCode
    )

    suspend fun setIncidentAudioPath(incidentId: Long, path: String?) {
        incidentDao.updateAudioFilePath(incidentId, path)
    }

    suspend fun setDemoMode(enabled: Boolean) = preferences.setDemoMode(enabled)
    suspend fun setLogRetentionDays(days: Int) = preferences.setLogRetentionDays(days)
    suspend fun deleteOldCompletedLogs(cutoff: Long) = incidentDao.deleteOldCompletedLogs(cutoff)
    suspend fun saveProfile(profile: UserProfile) = preferences.saveProfile(profile)
    suspend fun saveAppearance(settings: AppearanceSettings) = preferences.saveAppearance(settings)
}
