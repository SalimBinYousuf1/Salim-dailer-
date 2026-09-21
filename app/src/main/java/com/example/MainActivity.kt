package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.MainAppScreen

class MainActivity : ComponentActivity() {

    private var dialNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractDialNumber(intent)

        setContent {
            MainAppScreen(initialNumberFromIntent = dialNumber)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDialNumber(intent)
    }

    private fun extractDialNumber(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && (data.scheme == "tel" || data.scheme == "voicemail")) {
            dialNumber = data.schemeSpecificPart
        } else {
            val extra = intent?.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            if (!extra.isNullOrBlank()) {
                dialNumber = extra
            }
        }
    }
}

