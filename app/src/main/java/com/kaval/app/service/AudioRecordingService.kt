package com.kaval.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kaval.app.KavalApplication
import com.kaval.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AudioRecordingService : Service() {
    private var recorder: MediaRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var outputFile: File? = null
    private var incidentId: Long = -1L
    private var recordingStarted = false
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (recordingStarted) return START_STICKY

        incidentId = intent?.getLongExtra(EXTRA_INCIDENT_ID, -1L) ?: -1L
        if (incidentId < 0L) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        return if (startRecording()) START_STICKY else {
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun startRecording(): Boolean = runCatching {
        val directory = File(filesDir, "sos_recordings").apply { mkdirs() }
        outputFile = File(directory, "sos_${System.currentTimeMillis()}.m4a")

        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Kaval:SosAudio")
            .apply { acquire(MAX_DURATION_MS) }

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
            setOutputFile(outputFile!!.absolutePath)
            setMaxDuration(MAX_DURATION_MS.toInt())
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) stopSelf()
            }
            prepare()
            start()
        }
        recordingStarted = true
        persistAudioPath(outputFile!!.absolutePath)
        true
    }.getOrElse {
        releaseResources(deleteInvalidFile = true)
        false
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle("SOS Recording active")
        .setContentText("Audio evidence is being saved privately on this device")
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
            "SOS evidence recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Visible notification while Kaval records local SOS audio"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        releaseResources(deleteInvalidFile = false)
        super.onDestroy()
    }

    private fun releaseResources(deleteInvalidFile: Boolean) {
        val activeRecorder = recorder
        recorder = null
        var invalid = deleteInvalidFile
        if (recordingStarted && activeRecorder != null) {
            runCatching { activeRecorder.stop() }.onFailure { invalid = true }
        }
        runCatching { activeRecorder?.reset() }
        runCatching { activeRecorder?.release() }
        recordingStarted = false

        if (invalid) {
            outputFile?.delete()
            if (incidentId >= 0) persistAudioPath(null)
        }
        outputFile = null
        wakeLock?.let { lock -> if (lock.isHeld) lock.release() }
        wakeLock = null
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
        private const val MAX_DURATION_MS = 10 * 60 * 1000L

        fun start(context: Context, incidentId: Long) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AudioRecordingService::class.java)
                    .putExtra(EXTRA_INCIDENT_ID, incidentId)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AudioRecordingService::class.java))
        }
    }
}
