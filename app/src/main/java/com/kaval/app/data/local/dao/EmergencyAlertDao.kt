package com.kaval.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaval.app.data.local.entities.EmergencyAlertEntity
import com.kaval.app.data.local.entities.SmsDeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyAlertDao {
    @Query("SELECT * FROM emergency_alerts ORDER BY timestamp DESC")
    fun observeAlerts(): Flow<List<EmergencyAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: EmergencyAlertEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveries(deliveries: List<SmsDeliveryEntity>)

    @Query("UPDATE sms_deliveries SET status = :status, updatedAt = :updatedAt WHERE alertId = :alertId AND contactId = :contactId")
    suspend fun updateDelivery(alertId: Long, contactId: Long, status: String, updatedAt: Long)

    @Query("""
        UPDATE emergency_alerts SET
            sentCount = (SELECT COUNT(*) FROM sms_deliveries WHERE alertId = :alertId AND status IN ('sent', 'delivered')),
            deliveredCount = (SELECT COUNT(*) FROM sms_deliveries WHERE alertId = :alertId AND status = 'delivered'),
            failedCount = (SELECT COUNT(*) FROM sms_deliveries WHERE alertId = :alertId AND status = 'failed'),
            contactsNotified = (SELECT COUNT(*) FROM sms_deliveries WHERE alertId = :alertId AND status IN ('sent', 'delivered')),
            smsStatus = CASE
                WHEN (SELECT COUNT(*) FROM sms_deliveries WHERE alertId = :alertId AND status = 'failed') = contactsAttempted THEN 'failed'
                WHEN (SELECT COUNT(*) FROM sms_deliveries WHERE alertId = :alertId AND status = 'delivered') = contactsAttempted THEN 'delivered'
                WHEN (SELECT COUNT(*) FROM sms_deliveries WHERE alertId = :alertId AND status IN ('sent', 'delivered')) > 0 THEN 'sent'
                ELSE 'queued'
            END
        WHERE id = :alertId
    """)
    suspend fun refreshDeliverySummary(alertId: Long)
}
