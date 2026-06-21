package com.kaval.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kaval.app.data.local.entities.IncidentContactStatusEntity
import com.kaval.app.data.local.entities.IncidentEntity
import com.kaval.app.data.local.entities.IncidentWithContacts
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Transaction
    @Query("SELECT * FROM incident_log ORDER BY timestamp DESC")
    fun observeIncidents(): Flow<List<IncidentWithContacts>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(incident: IncidentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactStatuses(statuses: List<IncidentContactStatusEntity>)

    @Query("UPDATE incident_contact_status SET status = :status, updatedAt = :updatedAt WHERE incidentId = :incidentId AND contactId = :contactId")
    suspend fun updateContactStatus(incidentId: Long, contactId: Long, status: String, updatedAt: Long)

    @Query("""
        UPDATE incident_log SET
            sentCount = (SELECT COUNT(*) FROM incident_contact_status WHERE incidentId = :incidentId AND status IN ('sent', 'delivered')),
            deliveredCount = (SELECT COUNT(*) FROM incident_contact_status WHERE incidentId = :incidentId AND status = 'delivered'),
            failedCount = (SELECT COUNT(*) FROM incident_contact_status WHERE incidentId = :incidentId AND status = 'failed'),
            contactsNotified = (SELECT COUNT(*) FROM incident_contact_status WHERE incidentId = :incidentId AND status IN ('sent', 'delivered')),
            smsStatus = CASE
                WHEN (SELECT COUNT(*) FROM incident_contact_status WHERE incidentId = :incidentId AND status = 'failed') = contactsAttempted THEN 'failed'
                WHEN (SELECT COUNT(*) FROM incident_contact_status WHERE incidentId = :incidentId AND status = 'delivered') = contactsAttempted THEN 'delivered'
                WHEN (SELECT COUNT(*) FROM incident_contact_status WHERE incidentId = :incidentId AND status IN ('sent', 'delivered')) > 0 THEN 'sent'
                ELSE 'queued'
            END
        WHERE id = :incidentId
    """)
    suspend fun refreshSummary(incidentId: Long)

    @Transaction
    suspend fun updateContactAndSummary(incidentId: Long, contactId: Long, status: String) {
        updateContactStatus(incidentId, contactId, status, System.currentTimeMillis())
        refreshSummary(incidentId)
    }

    @Query("DELETE FROM incident_contact_status WHERE incidentId IN (SELECT id FROM incident_log WHERE timestamp < :cutoff AND status != 'Active')")
    suspend fun deleteContactStatusesBefore(cutoff: Long)

    @Query("DELETE FROM incident_log WHERE timestamp < :cutoff AND status != 'Active'")
    suspend fun deleteCompletedIncidentsBefore(cutoff: Long)

    @Transaction
    suspend fun deleteOldCompletedLogs(cutoff: Long) {
        deleteContactStatusesBefore(cutoff)
        deleteCompletedIncidentsBefore(cutoff)
    }
}
