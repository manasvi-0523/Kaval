package com.kaval.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.kaval.app.presentation.KavalApp

class MainActivity : ComponentActivity() {
    private val openEmergency = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openEmergency.value = intent?.getBooleanExtra(EXTRA_OPEN_EMERGENCY, false) == true
        enableEdgeToEdge()
        setContent {
            KavalApp(
                openEmergencyMode = openEmergency.value,
                onEmergencyIntentConsumed = { openEmergency.value = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_EMERGENCY, false)) {
            openEmergency.value = true
        }
    }

    companion object {
        const val EXTRA_OPEN_EMERGENCY = "open_emergency"
    }
}