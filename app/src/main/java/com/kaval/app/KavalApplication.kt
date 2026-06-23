package com.kaval.app

import android.app.Application
import com.kaval.app.BuildConfig
import com.kaval.app.data.datastore.KavalPreferences
import com.kaval.app.data.local.database.KavalDatabase
import com.kaval.app.data.location.LocationTracker
import com.kaval.app.data.maintenance.LogCleanupScheduler
import com.kaval.app.data.repository.KavalRepository
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class KavalApplication : Application() {
    val database by lazy { KavalDatabase.create(this) }
    val preferences by lazy { KavalPreferences(this) }
    val locationTracker by lazy { LocationTracker(this) }
    val repository by lazy {
        KavalRepository(
            contactDao = database.trustedContactDao(),
            incidentDao = database.incidentDao(),
            preferences = preferences
        )
    }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this, BuildConfig.MAPTILER_KEY, WellKnownTileServer.MapTiler)
        LogCleanupScheduler.schedule(this)
    }
}
