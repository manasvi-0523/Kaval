package com.kaval.app.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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
    val title: String,
    val message: String,
    val status: String,
    val locationLabel: String,
    val contactsNotified: Int,
    val isDemo: Boolean,
    val locationStatus: String,
    val mapsLink: String?,
    val audioFilePath: String?,
    val smsStatus: String,
    val sentCount: Int,
    val deliveredCount: Int,
    val failedCount: Int,
    val contactsAttempted: Int,
    val permissionStatus: String,
    val errorReason: String?,
    val exportedBeforeDelete: Boolean,
    val expiresAtEpochMillis: Long?
)

@Entity(
    tableName = "sms_deliveries",
    indices = [Index(value = ["incidentId", "contactId", "messageType"], unique = true)]
)
data class SmsDeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incidentId: Long,
    val contactId: Long,
    val contactName: String,
    val phoneNumber: String,
    val messageType: String,
    val sentStatus: String = "PENDING",
    val deliveryStatus: String = "PENDING",
    val sentAtEpochMillis: Long? = null,
    val deliveredAtEpochMillis: Long? = null,
    val failureReason: String? = null,
    val resultCode: Int? = null
)

data class IncidentWithContacts(
    @Embedded val incident: IncidentEntity,
    @Relation(parentColumn = "id", entityColumn = "incidentId")
    val contacts: List<SmsDeliveryEntity>
)

fun TrustedContactEntity.toDomain() = TrustedContact(id, name, phoneNumber, relationship, isPrimary)
fun TrustedContact.toEntity() = TrustedContactEntity(id, name, phoneNumber, relationship, isPrimary)

fun IncidentWithContacts.toDomain() = EmergencyAlert(
    id = incident.id,
    type = incident.type,
    timestamp = incident.timestamp,
    title = incident.title,
    message = incident.message,
    status = incident.status,
    locationLabel = incident.locationLabel,
    contactsNotified = incident.contactsNotified,
    isDemo = incident.isDemo,
    locationStatus = incident.locationStatus,
    mapsLink = incident.mapsLink,
    audioFilePath = incident.audioFilePath,
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
            messageType = contact.messageType,
            sentStatus = contact.sentStatus,
            deliveryStatus = contact.deliveryStatus,
            sentAtEpochMillis = contact.sentAtEpochMillis,
            deliveredAtEpochMillis = contact.deliveredAtEpochMillis,
            failureReason = contact.failureReason,
            resultCode = contact.resultCode
        )
    }
)

fun EmergencyAlert.toEntity() = IncidentEntity(
    id = id,
    type = type,
    timestamp = timestamp,
    title = title,
    message = message,
    status = status,
    locationLabel = locationLabel,
    contactsNotified = contactsNotified,
    isDemo = isDemo,
    locationStatus = locationStatus,
    mapsLink = mapsLink,
    audioFilePath = audioFilePath,
    smsStatus = smsStatus,
    sentCount = sentCount,
    deliveredCount = deliveredCount,
    failedCount = failedCount,
    contactsAttempted = contactsAttempted,
    permissionStatus = permissionStatus,
    errorReason = errorReason,
    exportedBeforeDelete = false,
    expiresAtEpochMillis = null
)