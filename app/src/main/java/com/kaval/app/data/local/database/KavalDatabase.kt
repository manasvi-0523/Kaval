package com.kaval.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kaval.app.data.local.dao.EmergencyAlertDao
import com.kaval.app.data.local.dao.TrustedContactDao
import com.kaval.app.data.local.entities.EmergencyAlertEntity
import com.kaval.app.data.local.entities.TrustedContactEntity

@Database(
    entities = [TrustedContactEntity::class, EmergencyAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KavalDatabase : RoomDatabase() {
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun emergencyAlertDao(): EmergencyAlertDao

    companion object {
        fun create(context: Context): KavalDatabase = Room.databaseBuilder(
            context.applicationContext,
            KavalDatabase::class.java,
            "kaval.db"
        ).build()
    }
}
