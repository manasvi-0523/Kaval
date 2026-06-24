package com.kaval.app.data.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.kaval.app.KavalApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsDeliveryStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val incidentId = intent.getLongExtra(EXTRA_INCIDENT_ID, -1L)
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val messageType = intent.getStringExtra(EXTRA_MESSAGE_TYPE) ?: return
        val isFinalPart = intent.getBooleanExtra(EXTRA_FINAL_PART, false)
        val carrierErrorCode = intent.getIntExtra("errorCode", Int.MIN_VALUE)
        if (incidentId < 0 || contactId < 0) return
        if (!isFinalPart) return

        val pendingResult = goAsync()
        val repository = (context.applicationContext as KavalApplication).repository
        receiverScope.launch {
            try {
                when (intent.action) {
                    ACTION_SMS_SENT -> {
                        val isConfirmedOrDeviceAccepted =
                            resultCode == Activity.RESULT_OK ||
                                (resultCode == Activity.RESULT_CANCELED && carrierErrorCode == Int.MIN_VALUE)
                        repository.updateSmsSent(
                            incidentId = incidentId,
                            contactId = contactId,
                            messageType = messageType,
                            status = if (isConfirmedOrDeviceAccepted) "SENT" else "FAILED",
                            failureReason = if (isConfirmedOrDeviceAccepted) null else sentFailureReason(resultCode, carrierErrorCode),
                            resultCode = resultCode
                        )
                    }
                    ACTION_SMS_DELIVERED -> repository.updateSmsDelivery(
                        incidentId = incidentId,
                        contactId = contactId,
                        messageType = messageType,
                        status = if (resultCode == Activity.RESULT_OK) "DELIVERED" else "DELIVERY_UNKNOWN",
                        failureReason = null,
                        resultCode = resultCode
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sentFailureReason(code: Int, carrierErrorCode: Int): String {
        val baseReason = when (code) {
        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Generic SMS failure"
        SmsManager.RESULT_ERROR_NO_SERVICE -> "No cellular service"
        SmsManager.RESULT_ERROR_NULL_PDU -> "Invalid SMS payload"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "Cellular radio is off"
        SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "SMS sending limit exceeded"
        SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED -> "Short-code sending not allowed"
        SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED -> "Short-code sending blocked"
        else -> "SMS failed with result code $code"
        }
        return if (carrierErrorCode != Int.MIN_VALUE) {
            "$baseReason; carrier errorCode=$carrierErrorCode"
        } else {
            baseReason
        }
    }

    companion object {
        const val ACTION_SMS_SENT = "com.kaval.app.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.kaval.app.SMS_DELIVERED"
        const val EXTRA_INCIDENT_ID = "incident_id"
        const val EXTRA_CONTACT_ID = "contact_id"
        const val EXTRA_MESSAGE_TYPE = "message_type"
        const val EXTRA_FINAL_PART = "final_part"

        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
