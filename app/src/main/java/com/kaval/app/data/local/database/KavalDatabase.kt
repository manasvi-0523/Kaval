package com.kaval.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kaval.app.data.local.dao.IncidentDao
import com.kaval.app.data.local.dao.TrustedContactDao
import com.kaval.app.data.local.entities.IncidentEntity
import com.kaval.app.data.local.entities.SmsDeliveryEntity
import com.kaval.app.data.local.entities.TrustedContactEntity

@Database(
    entities = [TrustedContactEntity::class, IncidentEntity::class, SmsDeliveryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class KavalDatabase : RoomDatabase() {
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun incidentDao(): IncidentDao

    companion object {
        fun create(context: Context): KavalDatabase = Room.databaseBuilder(
            context.applicationContext,
            KavalDatabase::class.java,
            "kaval.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS incident_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        locationLabel TEXT NOT NULL,
                        contactsNotified INTEGER NOT NULL,
                        isDemo INTEGER NOT NULL,
                        locationStatus TEXT NOT NULL,
                        mapsLink TEXT,
                        smsStatus TEXT NOT NULL,
                        sentCount INTEGER NOT NULL,
                        deliveredCount INTEGER NOT NULL,
                        failedCount INTEGER NOT NULL,
                        contactsAttempted INTEGER NOT NULL,
                        permissionStatus TEXT NOT NULL,
                        errorReason TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO incident_log
                    SELECT id, type, timestamp, status, locationLabel, contactsNotified, isDemo,
                           locationStatus, mapsLink, smsStatus, sentCount, deliveredCount, failedCount,
                           contactsAttempted, permissionStatus, errorReason
                    FROM emergency_alerts
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS incident_contact_status (
                        incidentId INTEGER NOT NULL,
                        contactId INTEGER NOT NULL,
                        contactName TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        status TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(incidentId, contactId)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO incident_contact_status
                    SELECT alertId, contactId, contactName, '', status, updatedAt
                    FROM sms_deliveries
                """.trimIndent())
                db.execSQL("DROP TABLE sms_deliveries")
                db.execSQL("DROP TABLE emergency_alerts")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE incident_log ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE incident_log ADD COLUMN message TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE incident_log ADD COLUMN audioFilePath TEXT")
                db.execSQL("ALTER TABLE incident_log ADD COLUMN exportedBeforeDelete INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE incident_log ADD COLUMN expiresAtEpochMillis INTEGER")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sms_deliveries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        incidentId INTEGER NOT NULL,
                        contactId INTEGER NOT NULL,
                        contactName TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        messageType TEXT NOT NULL,
                        sentStatus TEXT NOT NULL,
                        deliveryStatus TEXT NOT NULL,
                        sentAtEpochMillis INTEGER,
                        deliveredAtEpochMillis INTEGER,
                        failureReason TEXT,
                        resultCode INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO sms_deliveries (
                        incidentId, contactId, contactName, phoneNumber, messageType,
                        sentStatus, deliveryStatus, sentAtEpochMillis, deliveredAtEpochMillis
                    )
                    SELECT incidentId, contactId, contactName, phoneNumber, 'SOS',
                           CASE WHEN status IN ('sent', 'delivered') THEN 'SENT'
                                WHEN status = 'failed' THEN 'FAILED' ELSE 'PENDING' END,
                           CASE WHEN status = 'delivered' THEN 'DELIVERED'
                                WHEN status = 'failed' THEN 'DELIVERY_UNKNOWN' ELSE 'PENDING' END,
                           updatedAt,
                           CASE WHEN status = 'delivered' THEN updatedAt ELSE NULL END
                    FROM incident_contact_status
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sms_deliveries_incidentId_contactId_messageType ON sms_deliveries (incidentId, contactId, messageType)")
                db.execSQL("DROP TABLE incident_contact_status")
            }
        }
    }
}