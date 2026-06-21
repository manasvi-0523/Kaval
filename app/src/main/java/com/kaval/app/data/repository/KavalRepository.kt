package com.kaval.app.data.repository

import com.kaval.app.data.datastore.KavalPreferences
import com.kaval.app.data.local.dao.EmergencyAlertDao
import com.kaval.app.data.local.dao.TrustedContactDao
import com.kaval.app.data.local.entities.toDomain
import com.kaval.app.data.local.entities.toEntity
import com.kaval.app.data.local.entities.SmsDeliveryEntity
import com.kaval.app.domain.model.AppearanceSettings
import com.kaval.app.domain.model.EmergencyAlert
import com.kaval.app.domain.model.TrustedContact
import com.kaval.app.domain.model.UserProfile
import kotlinx.coroutines.flow.map

class KavalRepository(
    private val contactDao: TrustedContactDao,
    private val alertDao: EmergencyAlertDao,
    private val preferences: KavalPreferences
) {
    val contacts = contactDao.observeContacts().map { items -> items.map { it.toDomain() } }
    val alerts = alertDao.observeAlerts().map { items -> items.map { it.toDomain() } }
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

    suspend fun saveAlert(alert: EmergencyAlert): Long = alertDao.insert(alert.toEntity())

    suspend fun initializeSmsDeliveries(alertId: Long, contacts: List<TrustedContact>) {
        alertDao.insertDeliveries(
            contacts.map { contact ->
                SmsDeliveryEntity(alertId, contact.id, contact.name)
            }
        )
    }

    suspend fun updateSmsDelivery(alertId: Long, contactId: Long, status: String) {
        alertDao.updateDelivery(alertId, contactId, status, System.currentTimeMillis())
        alertDao.refreshDeliverySummary(alertId)
    }

    suspend fun setDemoMode(enabled: Boolean) = preferences.setDemoMode(enabled)

    suspend fun setLogRetentionDays(days: Int) = preferences.setLogRetentionDays(days)

    suspend fun deleteOldCompletedLogs(cutoff: Long) = alertDao.deleteOldCompletedLogs(cutoff)

    suspend fun saveProfile(profile: UserProfile) = preferences.saveProfile(profile)

    suspend fun saveAppearance(settings: AppearanceSettings) = preferences.saveAppearance(settings)
}
