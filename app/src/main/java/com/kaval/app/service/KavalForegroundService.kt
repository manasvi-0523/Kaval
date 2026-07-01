package com.kaval.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kaval.app.KavalApplication
import com.kaval.app.MainActivity
import com.kaval.app.data.remote.TrackingUploadStatus
import com.kaval.app.domain.model.KavalLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.File

class KavalForegroundService : Service() {
    private var recorder: MediaRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var outputFile: File? = null
    private var incidentId: Long = -1L
    private var trackingToken: String? = null
    private var displayName: String = "Kaval User"
    private var recordAudio: Boolean = false
    private var recordingStarted = false
    private var chunkIndex = 0
    private var startedAtMillis = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val trackingClient by lazy { (application as KavalApplication).trackingClient }
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_AUDIO) {
            stopAudioRecording()
            return START_STICKY
        }
        if (recordingStarted || locationCallback != null) return START_STICKY

        incidentId = intent?.getLongExtra(EXTRA_INCIDENT_ID, -1L) ?: -1L
        trackingToken = intent?.getStringExtra(EXTRA_TRACKING_TOKEN)
        displayName = intent?.getStringExtra(EXTRA_DISPLAY_NAME).orEmpty().ifBlank { "Kaval User" }
        recordAudio = intent?.getBooleanExtra(EXTRA_RECORD_AUDIO, false) == true
        if (incidentId < 0L) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        if (recordAudio) {
            startRecording()
        }
        startTracking()
        return START_STICKY
    }

    private fun startRecording(): Boolean = runCatching {
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Kaval:SosAudio")
            .apply { acquire(MAX_TOTAL_DURATION_MS) }
        startedAtMillis = System.currentTimeMillis()
        chunkIndex = 0
        startRecordingChunk()
        recordingStarted = true
        ioScope.launch {
            delay(MAX_TOTAL_DURATION_MS)
            mainHandler.post { stopAudioRecording() }
        }
        true
    }.getOrElse {
        releaseResources(deleteInvalidFile = true)
        false
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        val token = trackingToken ?: return
        if (!hasLocationPermission()) {
            TrackingUploadStatus.markFailure(IllegalStateException("Location permission unavailable"))
            return
        }
        if (!trackingClient.isBackendConfigured) {
            TrackingUploadStatus.markFailure(IllegalStateException("Tracking backend unavailable"))
            return
        }
        TrackingUploadStatus.markStarting()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val kavalLocation = location.toKavalLocation()
                ioScope.launch {
                    uploadLocationWithRetry(token, kavalLocation)
                }
            }
        }
        locationCallback = callback

        ioScope.launch {
            val initialLocation = runCatching { fusedClient.lastLocation.await() }
                .getOrNull()
                ?.toKavalLocation()
            val sessionStarted = trackingClient.startEmergencySession(
                token = token,
                displayName = displayName,
                location = initialLocation
            ).onSuccess {
                TrackingUploadStatus.markSuccess()
            }.onFailure {
                TrackingUploadStatus.markFailure(it)
            }.isSuccess
            if (sessionStarted) {
                fusedClient.requestLocationUpdates(
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
                        .setMinUpdateIntervalMillis(MIN_LOCATION_INTERVAL_MS)
                        .setMaxUpdateDelayMillis(LOCATION_INTERVAL_MS)
                        .build(),
                    callback,
                    Looper.getMainLooper()
                )
            }
        }
    }

    private suspend fun uploadLocationWithRetry(token: String, location: KavalLocation) {
        trackingClient.updateLocation(token, location)
            .onSuccess { TrackingUploadStatus.markSuccess(location.timestampMillis) }
            .onFailure { firstError ->
                TrackingUploadStatus.markFailure(firstError)
                delay(2_000L)
                trackingClient.updateLocation(token, location)
                    .onSuccess { TrackingUploadStatus.markSuccess(location.timestampMillis) }
                    .onFailure { TrackingUploadStatus.markFailure(it) }
            }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun Location.toKavalLocation() = KavalLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        timestampMillis = time.takeIf { it > 0L } ?: System.currentTimeMillis(),
        providerStatus = provider,
        mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
    )

    private fun startRecordingChunk() {
        val directory = File(filesDir, "sos/$incidentId").apply { mkdirs() }
        outputFile = File(
            directory,
            "sos_audio_${System.currentTimeMillis()}_${chunkIndex}.m4a"
        )
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(32_000)
            setOutputFile(outputFile!!.absolutePath)
            setMaxDuration(CHUNK_DURATION_MS.toInt())
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    mainHandler.post { rotateChunk() }
                }
            }
            prepare()
            start()
        }
        persistAudioPath(outputFile!!.absolutePath)
    }

    private fun rotateChunk() {
        if (!recordingStarted ||
            System.currentTimeMillis() - startedAtMillis >= MAX_TOTAL_DURATION_MS
        ) {
            stopSelf()
            return
        }
        releaseRecorder(deleteInvalidFile = false)
        chunkIndex += 1
        runCatching { startRecordingChunk() }.onFailure { stopSelf() }
    }

    private fun stopAudioRecording() {
        releaseRecorder(deleteInvalidFile = false)
        recordingStarted = false
        wakeLock?.let { lock -> if (lock.isHeld) lock.release() }
        wakeLock = null
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle("Emergency active")
        .setContentText(
            if (recordAudio) {
                "Live location sharing and local recording are active"
            } else {
                "Live location sharing is active"
            }
        )
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_OPEN_EMERGENCY, true)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Emergency safety service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Visible while Kaval shares emergency location or records local audio"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        releaseResources(deleteInvalidFile = false)
        super.onDestroy()
    }

    private fun releaseResources(deleteInvalidFile: Boolean) {
        mainHandler.removeCallbacksAndMessages(null)
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
        TrackingUploadStatus.markStopped()
        releaseRecorder(deleteInvalidFile)
        recordingStarted = false
        wakeLock?.let { lock -> if (lock.isHeld) lock.release() }
        wakeLock = null
    }

    private fun releaseRecorder(deleteInvalidFile: Boolean) {
        val activeRecorder = recorder
        recorder = null
        var invalid = deleteInvalidFile
        if (recordingStarted && activeRecorder != null) {
            runCatching { activeRecorder.stop() }.onFailure { invalid = true }
        }
        runCatching { activeRecorder?.reset() }
        runCatching { activeRecorder?.release() }
        if (invalid) {
            outputFile?.delete()
            if (incidentId >= 0) persistAudioPath(null)
        }
        outputFile = null
    }

    private fun persistAudioPath(path: String?) {
        val repository = (application as KavalApplication).repository
        ioScope.launch { repository.setIncidentAudioPath(incidentId, path) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "sos_recording"
        private const val NOTIFICATION_ID = 1201
        private const val EXTRA_INCIDENT_ID = "incident_id"
        private const val EXTRA_TRACKING_TOKEN = "tracking_token"
        private const val EXTRA_DISPLAY_NAME = "display_name"
        private const val EXTRA_RECORD_AUDIO = "record_audio"
        private const val ACTION_STOP_AUDIO = "com.kaval.app.STOP_SOS_AUDIO"
        private const val CHUNK_DURATION_MS = 60_000L
        private const val MAX_TOTAL_DURATION_MS = 10 * 60 * 1000L
        private const val LOCATION_INTERVAL_MS = 12_000L
        private const val MIN_LOCATION_INTERVAL_MS = 10_000L

        fun start(
            context: Context,
            incidentId: Long,
            trackingToken: String?,
            displayName: String,
            recordAudio: Boolean
        ) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, KavalForegroundService::class.java)
                    .putExtra(EXTRA_INCIDENT_ID, incidentId)
                    .putExtra(EXTRA_TRACKING_TOKEN, trackingToken)
                    .putExtra(EXTRA_DISPLAY_NAME, displayName)
                    .putExtra(EXTRA_RECORD_AUDIO, recordAudio)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KavalForegroundService::class.java))
        }

        fun stopRecording(context: Context) {
            context.startService(
                Intent(context, KavalForegroundService::class.java).setAction(ACTION_STOP_AUDIO)
            )
        }
    }
}
