package com.kaval.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaval.app.data.local.entities.EmergencyAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyAlertDao {
    @Query("SELECT * FROM emergency_alerts ORDER BY timestamp DESC")
    fun observeAlerts(): Flow<List<EmergencyAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: EmergencyAlertEntity): Long
}
