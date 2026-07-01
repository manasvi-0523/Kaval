package com.kaval.app.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackingUploadState(
    val active: Boolean = false,
    val lastSuccessAtMillis: Long? = null,
    val lastFailureAtMillis: Long? = null,
    val failureCount: Int = 0,
    val lastError: String? = null
) {
    val hasRecentFailure: Boolean
        get() = lastFailureAtMillis != null &&
            (lastSuccessAtMillis == null || lastFailureAtMillis > lastSuccessAtMillis)
}

object TrackingUploadStatus {
    private val mutableState = MutableStateFlow(TrackingUploadState())
    val state: StateFlow<TrackingUploadState> = mutableState.asStateFlow()

    fun markStarting() {
        mutableState.value = TrackingUploadState(active = true)
    }

    fun markSuccess(atMillis: Long = System.currentTimeMillis()) {
        mutableState.value = mutableState.value.copy(
            active = true,
            lastSuccessAtMillis = atMillis,
            lastError = null
        )
    }

    fun markFailure(error: Throwable, atMillis: Long = System.currentTimeMillis()) {
        val current = mutableState.value
        mutableState.value = current.copy(
            active = true,
            lastFailureAtMillis = atMillis,
            failureCount = current.failureCount + 1,
            lastError = error.message?.take(96) ?: error::class.java.simpleName
        )
    }

    fun markStopped() {
        mutableState.value = TrackingUploadState()
    }
}
