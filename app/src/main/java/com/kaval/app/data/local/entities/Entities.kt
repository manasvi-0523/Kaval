package com.kaval.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import com.kaval.app.domain.model.EmergencyAlert
import com.kaval.app.domain.model.SmsContactStatus
import com.kaval.app.domain.model.TrustedContact

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isPrimary: Boolean
)

@Entity(tableName = "incident_log")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val timestamp: Long,
    val status: String,
    val locationLabel: String,
    val contactsNotified: Int,
    val isDemo: Boolean,
    val locationStatus: String,
    val mapsLink: String?,
    val smsStatus: String,
    val sentCount: Int,
    val deliveredCount: Int,
    val failedCount: Int,
    val contactsAttempted: Int,
    val permissionStatus: String,
    val errorReason: String?
)

@Entity(tableName = "incident_contact_status", primaryKeys = ["incidentId", "contactId"])
data class IncidentContactStatusEntity(
    val incidentId: Long,
    val contactId: Long,
    val contactName: String,
    val phoneNumber: String,
    val status: String = "queued",
    val updatedAt: Long = System.currentTimeMillis()
)

data class IncidentWithContacts(
    @Embedded val incident: IncidentEntity,
    @Relation(parentColumn = "id", entityColumn = "incidentId")
    val contacts: List<IncidentContactStatusEntity>
)

fun TrustedContactEntity.toDomain() = TrustedContact(id, name, phoneNumber, relationship, isPrimary)
fun TrustedContact.toEntity() = TrustedContactEntity(id, name, phoneNumber, relationship, isPrimary)

fun IncidentWithContacts.toDomain() = EmergencyAlert(
    id = incident.id,
    type = incident.type,
    timestamp = incident.timestamp,
    status = incident.status,
    locationLabel = incident.locationLabel,
    contactsNotified = incident.contactsNotified,
    isDemo = incident.isDemo,
    locationStatus = incident.locationStatus,
    mapsLink = incident.mapsLink,
    smsStatus = incident.smsStatus,
    sentCount = incident.sentCount,
    deliveredCount = incident.deliveredCount,
    failedCount = incident.failedCount,
    contactsAttempted = incident.contactsAttempted,
    permissionStatus = incident.permissionStatus,
    errorReason = incident.errorReason,
    contactStatuses = contacts.map { contact ->
        SmsContactStatus(
            contactId = contact.contactId,
            contactName = contact.contactName,
            phoneNumber = contact.phoneNumber,
            status = contact.status,
            updatedAt = contact.updatedAt
        )
    }
)

fun EmergencyAlert.toEntity() = IncidentEntity(
    id = id,
    type = type,
    timestamp = timestamp,
    status = status,
    locationLabel = locationLabel,
    contactsNotified = contactsNotified,
    isDemo = isDemo,
    locationStatus = locationStatus,
    mapsLink = mapsLink,
    smsStatus = smsStatus,
    sentCount = sentCount,
    deliveredCount = deliveredCount,
    failedCount = failedCount,
    contactsAttempted = contactsAttempted,
    permissionStatus = permissionStatus,
    errorReason = errorReason
)
