package com.kaval.app.domain.model

data class KavalLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val timestampMillis: Long,
    val providerStatus: String?,
    val mapsLink: String
)

enum class LocationPermissionLevel {
    NONE,
    APPROXIMATE,
    PRECISE
}

enum class LocationStatus {
    PERMISSION_NEEDED,
    WAITING_FOR_GPS,
    LIVE,
    APPROXIMATE,
    STALE,
    UNAVAILABLE
}

data class KavalLocationState(
    val location: KavalLocation? = null,
    val permissionLevel: LocationPermissionLevel = LocationPermissionLevel.NONE,
    val status: LocationStatus = LocationStatus.PERMISSION_NEEDED,
    val message: String = "Location permission is needed for live safety features."
)

data class UserProfile(
    val name: String = "Kaval User",
    val phoneNumber: String = "",
    val emergencyNote: String = "I may be in danger. Please check on me immediately.",
    val bloodGroup: String? = null,
    val medicalNote: String? = null
)

data class TrustedContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isPrimary: Boolean
)

data class EmergencyAlert(
    val id: Long = 0,
    val type: String,
    val timestamp: Long,
    val status: String,
    val locationLabel: String,
    val contactsNotified: Int,
    val isDemo: Boolean
)

data class AppearanceSettings(
    val themeMode: String = "Dark",
    val visualStyle: String = "Protective Glass",
    val sosButtonStyle: String = "Raised Emergency Button + Pulse Ring",
    val colorIntensity: String = "Balanced",
    val motionLevel: String = "Standard Motion",
    val textSize: String = "Standard"
)

data class SafetyStatus(
    val status: String = "You are currently safe",
    val locationSharingActive: Boolean = false,
    val riskLevel: String = "Safe Zone"
)
