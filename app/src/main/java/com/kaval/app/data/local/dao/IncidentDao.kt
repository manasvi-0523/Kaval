package com.kaval.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kaval.app.data.local.entities.IncidentEntity
import com.kaval.app.data.local.entities.IncidentWithContacts
import com.kaval.app.data.local.entities.SmsDeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Transaction
    @Query("SELECT * FROM incident_log ORDER BY timestamp DESC")
    fun observeIncidents(): Flow<List<IncidentWithContacts>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(incident: IncidentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsDeliveries(statuses: List<SmsDeliveryEntity>)

    @Query("""
        UPDATE sms_deliveries SET
            sentStatus = CASE WHEN sentStatus = 'FAILED' THEN sentStatus ELSE :status END,
            sentAtEpochMillis = :timestamp,
            failureReason = :failureReason,
            resultCode = :resultCode
        WHERE incidentId = :incidentId AND contactId = :contactId AND messageType = :messageType
    """)
    suspend fun updateSentStatus(
        incidentId: Long,
        contactId: Long,
        messageType: String,
        status: String,
        timestamp: Long,
        failureReason: String?,
        resultCode: Int
    )

    @Query("""
        UPDATE sms_deliveries SET
            deliveryStatus = CASE WHEN deliveryStatus = 'DELIVERY_UNKNOWN' THEN deliveryStatus ELSE :status END,
            deliveredAtEpochMillis = :timestamp,
            failureReason = COALESCE(:failureReason, failureReason),
            resultCode = :resultCode
        WHERE incidentId = :incidentId AND contactId = :contactId AND messageType = :messageType
    """)
    suspend fun updateDeliveryStatus(
        incidentId: Long,
        contactId: Long,
        messageType: String,
        status: String,
        timestamp: Long,
        failureReason: String?,
        resultCode: Int
    )

    @Query("UPDATE incident_log SET audioFilePath = :path WHERE id = :incidentId")
    suspend fun updateAudioFilePath(incidentId: Long, path: String?)

    @Query("""
        UPDATE incident_log SET
            sentCount = (SELECT COUNT(*) FROM sms_deliveries WHERE incidentId = :incidentId AND sentStatus = 'SENT'),
            deliveredCount = (SELECT COUNT(*) FROM sms_deliveries WHERE incidentId = :incidentId AND deliveryStatus = 'DELIVERED'),
            failedCount = (SELECT COUNT(*) FROM sms_deliveries WHERE incidentId = :incidentId AND sentStatus = 'FAILED'),
            contactsNotified = (SELECT COUNT(*) FROM sms_deliveries WHERE incidentId = :incidentId AND sentStatus = 'SENT'),
            smsStatus = CASE
                WHEN (SELECT COUNT(*) FROM sms_deliveries WHERE incidentId = :incidentId AND sentStatus = 'FAILED') = contactsAttempted THEN 'failed'
                WHEN (SELECT COUNT(*) FROM sms_deliveries WHERE incidentId = :incidentId AND deliveryStatus = 'DELIVERED') = contactsAttempted THEN 'delivered'
                WHEN (SELECT COUNT(*) FROM sms_deliveries WHERE incidentId = :incidentId AND sentStatus = 'SENT') > 0 THEN 'sent'
                ELSE 'queued'
            END
        WHERE id = :incidentId
    """)
    suspend fun refreshSummary(incidentId: Long)

    @Transaction
    suspend fun updateSentAndSummary(
        incidentId: Long,
        contactId: Long,
        messageType: String,
        status: String,
        failureReason: String?,
        resultCode: Int
    ) {
        updateSentStatus(incidentId, contactId, messageType, status, System.currentTimeMillis(), failureReason, resultCode)
        refreshSummary(incidentId)
    }

    @Transaction
    suspend fun updateDeliveryAndSummary(
        incidentId: Long,
        contactId: Long,
        messageType: String,
        status: String,
        failureReason: String?,
        resultCode: Int
    ) {
        updateDeliveryStatus(incidentId, contactId, messageType, status, System.currentTimeMillis(), failureReason, resultCode)
        refreshSummary(incidentId)
    }

    @Query("DELETE FROM sms_deliveries WHERE incidentId IN (SELECT id FROM incident_log WHERE timestamp < :cutoff AND status != 'Active' AND audioFilePath IS NULL)")
    suspend fun deleteSmsDeliveriesBefore(cutoff: Long)

    @Query("DELETE FROM incident_log WHERE timestamp < :cutoff AND status != 'Active' AND audioFilePath IS NULL")
    suspend fun deleteCompletedIncidentsBefore(cutoff: Long)

    @Transaction
    suspend fun deleteOldCompletedLogs(cutoff: Long) {
        deleteSmsDeliveriesBefore(cutoff)
        deleteCompletedIncidentsBefore(cutoff)
    }
}