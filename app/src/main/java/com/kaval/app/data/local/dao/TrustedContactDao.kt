package com.kaval.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaval.app.data.local.entities.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY isPrimary DESC, name ASC")
    fun observeContacts(): Flow<List<TrustedContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: TrustedContactEntity): Long

    @Update
    suspend fun update(contact: TrustedContactEntity)

    @Delete
    suspend fun delete(contact: TrustedContactEntity)

    @Query("UPDATE trusted_contacts SET isPrimary = 0 WHERE id != :primaryId")
    suspend fun clearOtherPrimary(primaryId: Long)
}
