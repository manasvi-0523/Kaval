package com.kaval.app

import android.app.Application
import com.kaval.app.data.datastore.KavalPreferences
import com.kaval.app.data.local.database.KavalDatabase
import com.kaval.app.data.location.LocationTracker
import com.kaval.app.data.repository.KavalRepository

class KavalApplication : Application() {
    val database by lazy { KavalDatabase.create(this) }
    val preferences by lazy { KavalPreferences(this) }
    val locationTracker by lazy { LocationTracker(this) }
    val repository by lazy {
        KavalRepository(
            contactDao = database.trustedContactDao(),
            alertDao = database.emergencyAlertDao(),
            preferences = preferences
        )
    }
}
