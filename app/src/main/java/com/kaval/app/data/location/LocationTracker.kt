package com.kaval.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kaval.app.domain.model.KavalLocation
import com.kaval.app.domain.model.KavalLocationState
import com.kaval.app.domain.model.LocationPermissionLevel
import com.kaval.app.domain.model.LocationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

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

            val current = requestBestCurrentLocation(permissionLevel)

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

    @SuppressLint("MissingPermission")
    private suspend fun requestBestCurrentLocation(permissionLevel: LocationPermissionLevel): Location? {
        val priority = if (permissionLevel == LocationPermissionLevel.PRECISE) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val currentRequest = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setMaxUpdateAgeMillis(0L)
            .setDurationMillis(15_000L)
            .build()
        val cancellationTokenSource = CancellationTokenSource()
        val current = fusedClient.getCurrentLocation(currentRequest, cancellationTokenSource.token).await()

        if (permissionLevel != LocationPermissionLevel.PRECISE || current.isAccurateEnough()) {
            return current
        }

        var best = current
        val refined = withTimeoutOrNull(15_000L) {
            callbackFlow {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                    .setMinUpdateIntervalMillis(500L)
                    .setMaxUpdateDelayMillis(1_000L)
                    .setWaitForAccurateLocation(true)
                    .build()
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let(::trySend)
                    }
                }
                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                awaitClose { fusedClient.removeLocationUpdates(callback) }
            }.onEach { candidate ->
                if (best == null || candidate.accuracyOrMax() < best.accuracyOrMax()) {
                    best = candidate
                    publishImprovingAccuracy(candidate)
                }
            }.first { it.isAccurateEnough() }
        }
        return refined ?: best
    }

    private fun publishImprovingAccuracy(location: Location) {
        mutableState.value = mutableState.value.copy(
            location = location.toKavalLocation(),
            status = LocationStatus.WAITING_FOR_GPS,
            message = "Improving GPS accuracy. Keep the phone near an open view of the sky."
        )
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
        val location = androidLocation.toKavalLocation()
        val isApproximate = permissionLevel == LocationPermissionLevel.APPROXIMATE
        val isLowAccuracy = permissionLevel == LocationPermissionLevel.PRECISE && androidLocation.accuracyOrMax() > 50f
        mutableState.value = KavalLocationState(
            location = location,
            permissionLevel = permissionLevel,
            status = when {
                isApproximate -> LocationStatus.APPROXIMATE
                isLowAccuracy -> LocationStatus.STALE
                isFresh -> LocationStatus.LIVE
                else -> LocationStatus.STALE
            },
            message = when {
                isApproximate -> "Approximate location only. Accuracy may be limited."
                isLowAccuracy -> "GPS accuracy is limited. Move outdoors or near a window, then refresh."
                isFresh -> "Live location active."
                else -> "Showing the last known location while GPS refreshes."
            }
        )
    }

    private fun Location.toKavalLocation() = KavalLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        timestampMillis = time.takeIf { it > 0 } ?: System.currentTimeMillis(),
        providerStatus = provider,
        mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
    )

    private fun Location?.isAccurateEnough(): Boolean {
        return this != null && hasAccuracy() && accuracy <= 25f
    }

    private fun Location?.accuracyOrMax(): Float {
        return if (this != null && hasAccuracy()) accuracy else Float.MAX_VALUE
    }

    private fun publishUnavailable(permissionLevel: LocationPermissionLevel) {
        mutableState.value = KavalLocationState(
            permissionLevel = permissionLevel,
            status = LocationStatus.UNAVAILABLE,
            message = "Location is unavailable. Check that device location is turned on."
        )
    }
}
