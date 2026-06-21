package com.kaval.app.data.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaval.app.KavalApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsDeliveryStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val incidentId = intent.getLongExtra(EXTRA_INCIDENT_ID, -1L)
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val isFinalPart = intent.getBooleanExtra(EXTRA_FINAL_PART, false)
        if (incidentId < 0 || contactId < 0) return

        if (!isFinalPart && resultCode == Activity.RESULT_OK) return

        val status = when (intent.action) {
            ACTION_SMS_SENT -> if (resultCode == Activity.RESULT_OK) "sent" else "failed"
            ACTION_SMS_DELIVERED -> if (resultCode == Activity.RESULT_OK) "delivered" else "failed"
            else -> return
        }
        val pendingResult = goAsync()
        val repository = (context.applicationContext as KavalApplication).repository
        receiverScope.launch {
            try {
                repository.updateSmsDelivery(incidentId, contactId, status)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SMS_SENT = "com.kaval.app.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.kaval.app.SMS_DELIVERED"
        const val EXTRA_INCIDENT_ID = "incident_id"
        const val EXTRA_CONTACT_ID = "contact_id"
        const val EXTRA_FINAL_PART = "final_part"

        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
