package com.kaval.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kaval.app.data.local.dao.EmergencyAlertDao
import com.kaval.app.data.local.dao.TrustedContactDao
import com.kaval.app.data.local.entities.EmergencyAlertEntity
import com.kaval.app.data.local.entities.TrustedContactEntity
import com.kaval.app.data.local.entities.SmsDeliveryEntity

@Database(
    entities = [TrustedContactEntity::class, EmergencyAlertEntity::class, SmsDeliveryEntity::class],
    version = 2,
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
        ).addMigrations(MIGRATION_1_2).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE emergency_alerts ADD COLUMN locationStatus TEXT NOT NULL DEFAULT 'unavailable'")
                db.execSQL("ALTER TABLE emergency_alerts ADD COLUMN mapsLink TEXT")
                db.execSQL("ALTER TABLE emergency_alerts ADD COLUMN smsStatus TEXT NOT NULL DEFAULT 'queued'")
                db.execSQL("ALTER TABLE emergency_alerts ADD COLUMN sentCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE emergency_alerts ADD COLUMN deliveredCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE emergency_alerts ADD COLUMN failedCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE emergency_alerts ADD COLUMN contactsAttempted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE emergency_alerts ADD COLUMN permissionStatus TEXT NOT NULL DEFAULT 'unknown'")
                db.execSQL("ALTER TABLE emergency_alerts ADD COLUMN errorReason TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS sms_deliveries (alertId INTEGER NOT NULL, contactId INTEGER NOT NULL, contactName TEXT NOT NULL, status TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(alertId, contactId))")
            }
        }
    }
}
