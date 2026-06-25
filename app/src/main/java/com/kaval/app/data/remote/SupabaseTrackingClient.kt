package com.kaval.app.data.remote

import android.content.Context
import com.kaval.app.BuildConfig
import com.kaval.app.domain.model.KavalLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.security.SecureRandom

class SupabaseTrackingClient(context: Context) {
    private val store = SupabaseSessionStore(context.applicationContext)
    private val authMutex = Mutex()

    val isBackendConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val isGuardianWebConfigured: Boolean
        get() = BuildConfig.GUARDIAN_WEB_BASE_URL.isNotBlank()

    fun newTrackingToken(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun trackingUrl(token: String): String? {
        if (!isGuardianWebConfigured) return null
        return "${BuildConfig.GUARDIAN_WEB_BASE_URL.trimEnd('/')}/track/$token"
    }

    suspend fun startEmergencySession(
        token: String,
        displayName: String,
        location: KavalLocation?
    ): Result<Unit> = runCatching {
        require(isBackendConfigured) { "Supabase tracking is not configured." }
        val session = authenticatedSession()
        upsertProfile(session, displayName)

        val body = JSONArray().put(
            JSONObject()
                .put("user_id", session.userId)
                .put("destination_name", "Emergency location sharing")
                .put("status", "emergency")
                .put("session_token", token)
                .put("last_lat", location?.latitude ?: JSONObject.NULL)
                .put("last_lng", location?.longitude ?: JSONObject.NULL)
                .put("last_accuracy_meters", location?.accuracyMeters?.toInt() ?: JSONObject.NULL)
                .put("last_updated_at", if (location != null) isoNow() else JSONObject.NULL)
        )
        request(
            method = "POST",
            path = "/rest/v1/journey_sessions",
            accessToken = session.accessToken,
            body = body.toString(),
            prefer = "return=minimal"
        )
        store.setActiveTrackingToken(token)
    }

    suspend fun updateLocation(token: String, location: KavalLocation): Result<Unit> = runCatching {
        val session = authenticatedSession()
        val body = JSONObject()
            .put("last_lat", location.latitude)
            .put("last_lng", location.longitude)
            .put("last_accuracy_meters", location.accuracyMeters?.toInt() ?: JSONObject.NULL)
            .put("last_updated_at", isoNow())
        request(
            method = "PATCH",
            path = "/rest/v1/journey_sessions?session_token=eq.$token&user_id=eq.${session.userId}",
            accessToken = session.accessToken,
            body = body.toString(),
            prefer = "return=minimal"
        )
    }

    suspend fun completeActiveSession(): Result<Unit> = runCatching {
        val token = store.activeTrackingToken() ?: return@runCatching
        val session = authenticatedSession()
        val body = JSONObject()
            .put("status", "completed")
            .put("expires_at", isoNow())
        request(
            method = "PATCH",
            path = "/rest/v1/journey_sessions?session_token=eq.$token&user_id=eq.${session.userId}",
            accessToken = session.accessToken,
            body = body.toString(),
            prefer = "return=minimal"
        )
        store.setActiveTrackingToken(null)
    }

    fun activeTrackingToken(): String? = store.activeTrackingToken()

    private suspend fun authenticatedSession(): SupabaseAuthSession = authMutex.withLock {
        val current = store.readAuthSession()
        if (current != null && current.expiresAtMillis > System.currentTimeMillis() + 60_000L) {
            return@withLock current
        }

        if (current != null) {
            runCatching { refreshSession(current.refreshToken) }
                .onSuccess {
                    store.saveAuthSession(it)
                    return@withLock it
                }
                .onFailure { store.clearAuthSession() }
        }

        createAnonymousSession().also(store::saveAuthSession)
    }

    private suspend fun createAnonymousSession(): SupabaseAuthSession {
        val response = request(
            method = "POST",
            path = "/auth/v1/signup",
            accessToken = BuildConfig.SUPABASE_ANON_KEY,
            body = "{}"
        )
        return response.toAuthSession()
    }

    private suspend fun refreshSession(refreshToken: String): SupabaseAuthSession {
        val response = request(
            method = "POST",
            path = "/auth/v1/token?grant_type=refresh_token",
            accessToken = BuildConfig.SUPABASE_ANON_KEY,
            body = JSONObject().put("refresh_token", refreshToken).toString()
        )
        return response.toAuthSession()
    }

    private suspend fun upsertProfile(session: SupabaseAuthSession, displayName: String) {
        val body = JSONArray().put(
            JSONObject()
                .put("id", session.userId)
                .put("display_name", displayName.ifBlank { "Kaval User" })
        )
        request(
            method = "POST",
            path = "/rest/v1/users?on_conflict=id",
            accessToken = session.accessToken,
            body = body.toString(),
            prefer = "resolution=merge-duplicates,return=minimal"
        )
    }

    private suspend fun request(
        method: String,
        path: String,
        accessToken: String,
        body: String? = null,
        prefer: String? = null
    ): String = withContext(Dispatchers.IO) {
        val connection = URI("${BuildConfig.SUPABASE_URL.trimEnd('/')}$path")
            .toURL()
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json")
            prefer?.let { connection.setRequestProperty("Prefer", it) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }

            val status = connection.responseCode
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                throw IOException("Supabase request failed ($status): ${responseBody.take(240)}")
            }
            responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun String.toAuthSession(): SupabaseAuthSession {
        val json = JSONObject(this)
        val user = json.getJSONObject("user")
        val expiresInSeconds = json.optLong("expires_in", 3600L)
        return SupabaseAuthSession(
            userId = user.getString("id"),
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            expiresAtMillis = System.currentTimeMillis() + expiresInSeconds * 1_000L
        )
    }

    private fun isoNow(): String = java.time.Instant.now().toString()
}
