package com.example.ui.incall

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.data.model.SalimCallState
import com.example.telecom.CallManager
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and show over lockscreen for calls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        CallManager.callState.onEach { state ->
            if (state.state == SalimCallState.DISCONNECTED || state.state == SalimCallState.IDLE) {
                delay(1800)
                if (!isFinishing && !isDestroyed) {
                    finish()
                }
            }
        }.launchIn(lifecycleScope)

        setContent {
            MyApplicationTheme(darkTheme = true, trueBlack = true) {
                InCallScreen(onClose = { finish() })
            }
        }
    }
}
