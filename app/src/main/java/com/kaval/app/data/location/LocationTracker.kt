package com.kaval.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kaval.app.domain.model.KavalLocation
import com.kaval.app.domain.model.KavalLocationState
import com.kaval.app.domain.model.LocationPermissionLevel
import com.kaval.app.domain.model.LocationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class LocationTracker(context: Context) {
    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val mutableState = MutableStateFlow(KavalLocationState())

    val state: StateFlow<KavalLocationState> = mutableState.asStateFlow()

    fun refreshPermissionState() {
        val permissionLevel = currentPermissionLevel()
        if (permissionLevel == LocationPermissionLevel.NONE) {
            mutableState.value = KavalLocationState()
        } else {
            mutableState.value = mutableState.value.copy(
                permissionLevel = permissionLevel,
                status = LocationStatus.WAITING_FOR_GPS,
                message = "Waiting for a location update."
            )
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun refreshLocation() {
        val permissionLevel = currentPermissionLevel()
        if (permissionLevel == LocationPermissionLevel.NONE) {
            mutableState.value = KavalLocationState()
            return
        }

        mutableState.value = mutableState.value.copy(
            permissionLevel = permissionLevel,
            status = LocationStatus.WAITING_FOR_GPS,
            message = "Waiting for GPS. This can take longer indoors."
        )

        var lastKnown: Location? = null
        try {
            lastKnown = fusedClient.lastLocation.await()
            lastKnown?.let { publishLocation(it, permissionLevel, isFresh = false) }

            val priority = if (permissionLevel == LocationPermissionLevel.PRECISE) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }
            val cancellationTokenSource = CancellationTokenSource()
            val current = fusedClient.getCurrentLocation(priority, cancellationTokenSource.token).await()

            if (current != null) {
                publishLocation(current, permissionLevel, isFresh = true)
            } else if (lastKnown == null) {
                publishUnavailable(permissionLevel)
            }
        } catch (_: SecurityException) {
            mutableState.value = KavalLocationState()
        } catch (_: Exception) {
            if (lastKnown == null) {
                publishUnavailable(permissionLevel)
            } else {
                publishLocation(lastKnown, permissionLevel, isFresh = false)
            }
        }
    }

    private fun currentPermissionLevel(): LocationPermissionLevel {
        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine) return LocationPermissionLevel.PRECISE

        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return if (hasCoarse) LocationPermissionLevel.APPROXIMATE else LocationPermissionLevel.NONE
    }

    private fun publishLocation(
        androidLocation: Location,
        permissionLevel: LocationPermissionLevel,
        isFresh: Boolean
    ) {
        val location = KavalLocation(
            latitude = androidLocation.latitude,
            longitude = androidLocation.longitude,
            accuracyMeters = if (androidLocation.hasAccuracy()) androidLocation.accuracy else null,
            timestampMillis = androidLocation.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
            providerStatus = androidLocation.provider,
            mapsLink = "https://maps.google.com/?q=${androidLocation.latitude},${androidLocation.longitude}"
        )
        val isApproximate = permissionLevel == LocationPermissionLevel.APPROXIMATE
        mutableState.value = KavalLocationState(
            location = location,
            permissionLevel = permissionLevel,
            status = when {
                isApproximate -> LocationStatus.APPROXIMATE
                isFresh -> LocationStatus.LIVE
                else -> LocationStatus.STALE
            },
            message = when {
                isApproximate -> "Approximate location only. Accuracy may be limited."
                isFresh -> "Live location active."
                else -> "Showing the last known location while GPS refreshes."
            }
        )
    }

    private fun publishUnavailable(permissionLevel: LocationPermissionLevel) {
        mutableState.value = KavalLocationState(
            permissionLevel = permissionLevel,
            status = LocationStatus.UNAVAILABLE,
            message = "Location is unavailable. Check that device location is turned on."
        )
    }
}
