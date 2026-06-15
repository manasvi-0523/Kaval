package com.kaval.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kaval.app.domain.model.EmergencyAlert
import com.kaval.app.domain.model.TrustedContact

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isPrimary: Boolean
)

@Entity(tableName = "emergency_alerts")
data class EmergencyAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val timestamp: Long,
    val status: String,
    val locationLabel: String,
    val contactsNotified: Int,
    val isDemo: Boolean
)

fun TrustedContactEntity.toDomain() = TrustedContact(id, name, phoneNumber, relationship, isPrimary)
fun TrustedContact.toEntity() = TrustedContactEntity(id, name, phoneNumber, relationship, isPrimary)

fun EmergencyAlertEntity.toDomain() = EmergencyAlert(
    id = id,
    type = type,
    timestamp = timestamp,
    status = status,
    locationLabel = locationLabel,
    contactsNotified = contactsNotified,
    isDemo = isDemo
)

fun EmergencyAlert.toEntity() = EmergencyAlertEntity(
    id = id,
    type = type,
    timestamp = timestamp,
    status = status,
    locationLabel = locationLabel,
    contactsNotified = contactsNotified,
    isDemo = isDemo
)
