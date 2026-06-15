package com.kaval.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kaval.app.domain.model.AppearanceSettings
import com.kaval.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kavalDataStore by preferencesDataStore("kaval_preferences")

class KavalPreferences(private val context: Context) {
    val demoMode: Flow<Boolean> = context.kavalDataStore.data.map { it[Keys.demoMode] ?: true }

    val profile: Flow<UserProfile> = context.kavalDataStore.data.map {
        UserProfile(
            name = it[Keys.name] ?: "Kaval User",
            phoneNumber = it[Keys.phone] ?: "",
            emergencyNote = it[Keys.emergencyNote]
                ?: "I may be in danger. Please check on me immediately.",
            bloodGroup = it[Keys.bloodGroup],
            medicalNote = it[Keys.medicalNote]
        )
    }

    val appearance: Flow<AppearanceSettings> = context.kavalDataStore.data.map {
        AppearanceSettings(
            themeMode = it[Keys.themeMode] ?: "Dark",
            visualStyle = it[Keys.visualStyle] ?: "Protective Glass",
            sosButtonStyle = it[Keys.sosButtonStyle] ?: "Raised Emergency Button + Pulse Ring",
            colorIntensity = it[Keys.colorIntensity] ?: "Balanced",
            motionLevel = it[Keys.motionLevel] ?: "Standard Motion",
            textSize = it[Keys.textSize] ?: "Standard"
        )
    }

    suspend fun setDemoMode(enabled: Boolean) {
        context.kavalDataStore.edit { it[Keys.demoMode] = enabled }
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.kavalDataStore.edit {
            it[Keys.name] = profile.name
            it[Keys.phone] = profile.phoneNumber
            it[Keys.emergencyNote] = profile.emergencyNote
            it[Keys.bloodGroup] = profile.bloodGroup.orEmpty()
            it[Keys.medicalNote] = profile.medicalNote.orEmpty()
        }
    }

    suspend fun saveAppearance(settings: AppearanceSettings) {
        context.kavalDataStore.edit {
            it[Keys.themeMode] = settings.themeMode
            it[Keys.visualStyle] = settings.visualStyle
            it[Keys.sosButtonStyle] = settings.sosButtonStyle
            it[Keys.colorIntensity] = settings.colorIntensity
            it[Keys.motionLevel] = settings.motionLevel
            it[Keys.textSize] = settings.textSize
        }
    }

    private object Keys {
        val demoMode = booleanPreferencesKey("demo_mode")
        val name = stringPreferencesKey("profile_name")
        val phone = stringPreferencesKey("profile_phone")
        val emergencyNote = stringPreferencesKey("profile_emergency_note")
        val bloodGroup = stringPreferencesKey("profile_blood_group")
        val medicalNote = stringPreferencesKey("profile_medical_note")
        val themeMode = stringPreferencesKey("theme_mode")
        val visualStyle = stringPreferencesKey("visual_style")
        val sosButtonStyle = stringPreferencesKey("sos_button_style")
        val colorIntensity = stringPreferencesKey("color_intensity")
        val motionLevel = stringPreferencesKey("motion_level")
        val textSize = stringPreferencesKey("text_size")
    }
}
