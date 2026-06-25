package com.kaval.app.data.remote

import android.content.Context

internal data class SupabaseAuthSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long
)

internal class SupabaseSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "kaval_supabase_session",
        Context.MODE_PRIVATE
    )

    fun readAuthSession(): SupabaseAuthSession? {
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return SupabaseAuthSession(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = preferences.getLong(KEY_EXPIRES_AT, 0L)
        )
    }

    fun saveAuthSession(session: SupabaseAuthSession) {
        preferences.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
            .apply()
    }

    fun clearAuthSession() {
        preferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    fun activeTrackingToken(): String? =
        preferences.getString(KEY_ACTIVE_TRACKING_TOKEN, null)

    fun setActiveTrackingToken(token: String?) {
        preferences.edit().apply {
            if (token == null) remove(KEY_ACTIVE_TRACKING_TOKEN)
            else putString(KEY_ACTIVE_TRACKING_TOKEN, token)
        }.apply()
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_ACTIVE_TRACKING_TOKEN = "active_tracking_token"
    }
}
