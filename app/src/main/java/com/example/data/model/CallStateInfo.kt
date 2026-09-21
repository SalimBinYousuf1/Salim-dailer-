package com.example.data.model

import android.net.Uri

enum class SalimCallState {
    IDLE,
    DIALING,
    RINGING,
    CONNECTING,
    ACTIVE,
    HOLDING,
    DISCONNECTED
}

enum class AudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET
}

data class CallStateInfo(
    val callId: String = "",
    val number: String = "",
    val displayName: String = "",
    val photoUri: Uri? = null,
    val state: SalimCallState = SalimCallState.IDLE,
    val durationSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isHold: Boolean = false,
    val audioRoute: AudioRoute = AudioRoute.EARPIECE,
    val availableAudioRoutes: List<AudioRoute> = listOf(AudioRoute.EARPIECE, AudioRoute.SPEAKER),
    val isIncoming: Boolean = false,
    val canMerge: Boolean = false,
    val canAddCall: Boolean = true,
    val disconnectReason: String? = null
)
