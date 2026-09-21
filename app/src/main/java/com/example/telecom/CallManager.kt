package com.example.telecom

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import com.example.data.model.AudioRoute
import com.example.data.model.CallStateInfo
import com.example.data.model.SalimCallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object CallManager {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private var currentCall: Call? = null
    private var secondCall: Call? = null
    private var inCallService: SalimInCallService? = null
    private var appContext: Context? = null

    private val _callState = MutableStateFlow(CallStateInfo())
    val callState: StateFlow<CallStateInfo> = _callState.asStateFlow()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            super.onStateChanged(call, state)
            updateStateFromTelecom()
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            super.onDetailsChanged(call, details)
            updateStateFromTelecom()
        }

        override fun onConferenceableCallsChanged(call: Call?, conferenceableCalls: MutableList<Call>?) {
            super.onConferenceableCallsChanged(call, conferenceableCalls)
            updateStateFromTelecom()
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun attachService(service: SalimInCallService) {
        inCallService = service
    }

    fun detachService() {
        inCallService = null
    }

    fun registerCall(call: Call) {
        if (currentCall == null) {
            currentCall = call
            call.registerCallback(callCallback)
            updateStateFromTelecom()
        } else if (secondCall == null && currentCall != call) {
            secondCall = call
            call.registerCallback(callCallback)
            updateStateFromTelecom()
        }
    }

    fun unregisterCall(call: Call) {
        call.unregisterCallback(callCallback)
        if (currentCall == call) {
            currentCall = secondCall
            secondCall = null
        } else if (secondCall == call) {
            secondCall = null
        }

        if (currentCall == null) {
            stopTimer()
            _callState.value = _callState.value.copy(
                state = SalimCallState.DISCONNECTED,
                disconnectReason = "Call Ended"
            )
            // After brief period reset to IDLE
            scope.launch {
                delay(2500)
                if (currentCall == null) {
                    _callState.value = CallStateInfo()
                }
            }
        } else {
            updateStateFromTelecom()
        }
    }

    fun onAudioStateChanged(audioState: CallAudioState) {
        val currentRoute = when (audioState.route) {
            CallAudioState.ROUTE_SPEAKER -> AudioRoute.SPEAKER
            CallAudioState.ROUTE_BLUETOOTH -> AudioRoute.BLUETOOTH
            CallAudioState.ROUTE_WIRED_HEADSET -> AudioRoute.WIRED_HEADSET
            else -> AudioRoute.EARPIECE
        }

        val available = mutableListOf<AudioRoute>()
        if (audioState.supportedRouteMask and CallAudioState.ROUTE_EARPIECE != 0) {
            available.add(AudioRoute.EARPIECE)
        }
        if (audioState.supportedRouteMask and CallAudioState.ROUTE_SPEAKER != 0) {
            available.add(AudioRoute.SPEAKER)
        }
        if (audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0) {
            available.add(AudioRoute.BLUETOOTH)
        }
        if (audioState.supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
            available.add(AudioRoute.WIRED_HEADSET)
        }

        _callState.value = _callState.value.copy(
            isMuted = audioState.isMuted,
            audioRoute = currentRoute,
            availableAudioRoutes = if (available.isNotEmpty()) available else listOf(AudioRoute.EARPIECE, AudioRoute.SPEAKER)
        )
    }

    fun answer() {
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject(rejectWithMessage: Boolean = false, textMessage: String? = null) {
        val call = currentCall ?: return
        if (rejectWithMessage && !textMessage.isNullOrBlank()) {
            call.reject(true, textMessage)
        } else {
            call.reject(false, null)
        }
    }

    fun disconnect() {
        currentCall?.disconnect()
    }

    fun toggleMute() {
        val service = inCallService ?: return
        val newMute = !_callState.value.isMuted
        service.setMuted(newMute)
        _callState.value = _callState.value.copy(isMuted = newMute)
    }

    fun setAudioRoute(route: AudioRoute) {
        val service = inCallService ?: return
        val telecomRoute = when (route) {
            AudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
            AudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
            AudioRoute.WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
            AudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
        }
        service.setAudioRoute(telecomRoute)
    }

    fun toggleSpeaker() {
        if (_callState.value.audioRoute == AudioRoute.SPEAKER) {
            setAudioRoute(AudioRoute.EARPIECE)
        } else {
            setAudioRoute(AudioRoute.SPEAKER)
        }
    }

    fun toggleHold() {
        val call = currentCall ?: return
        if (_callState.value.isHold) {
            call.unhold()
        } else {
            call.hold()
        }
    }

    fun playDtmf(digit: Char) {
        currentCall?.playDtmfTone(digit)
        DtmfPlayer.playTone(digit, 150)
        scope.launch {
            delay(150)
            currentCall?.stopDtmfTone()
        }
    }

    fun mergeCalls() {
        val first = currentCall ?: return
        val second = secondCall ?: return
        first.conference(second)
    }

    private fun updateStateFromTelecom() {
        val call = currentCall ?: return
        val details = call.details
        val rawState = call.state

        val salimState = when (rawState) {
            Call.STATE_DIALING -> SalimCallState.DIALING
            Call.STATE_RINGING -> SalimCallState.RINGING
            Call.STATE_CONNECTING -> SalimCallState.CONNECTING
            Call.STATE_ACTIVE -> SalimCallState.ACTIVE
            Call.STATE_HOLDING -> SalimCallState.HOLDING
            Call.STATE_DISCONNECTED -> SalimCallState.DISCONNECTED
            else -> SalimCallState.IDLE
        }

        val rawNumber = details?.handle?.schemeSpecificPart ?: ""
        val isIncoming = rawState == Call.STATE_RINGING

        val canMerge = secondCall != null

        val isHold = rawState == Call.STATE_HOLDING

        if (salimState == SalimCallState.ACTIVE && timerJob == null) {
            val connectTime = details?.connectTimeMillis ?: 0L
            val initialSeconds = if (connectTime > 0) {
                (System.currentTimeMillis() - connectTime) / 1000
            } else 0L
            startTimer(initialSeconds)
        } else if (salimState != SalimCallState.ACTIVE && salimState != SalimCallState.HOLDING) {
            stopTimer()
        }

        val currentInfo = _callState.value
        _callState.value = currentInfo.copy(
            callId = details?.handle?.toString() ?: "",
            number = rawNumber,
            state = salimState,
            isIncoming = isIncoming,
            isHold = isHold,
            canMerge = canMerge
        )

        // Resolve caller name/photo in background if not already set or unknown
        if (currentInfo.displayName.isBlank() || currentInfo.displayName == rawNumber) {
            scope.launch {
                val resolved = resolveContact(rawNumber)
                if (resolved.displayName.isNotBlank()) {
                    _callState.value = _callState.value.copy(
                        displayName = resolved.displayName,
                        photoUri = resolved.photoUri
                    )
                }
            }
        }
    }

    private data class CallerInfo(val displayName: String, val photoUri: Uri?)

    private suspend fun resolveContact(number: String): CallerInfo = withContext(Dispatchers.IO) {
        if (number.isBlank() || appContext == null) return@withContext CallerInfo("", null)
        var name = number
        var photoUri: Uri? = null
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val cursor = appContext?.contentResolver?.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
                ),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToNext()) {
                    val nameIdx = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIdx = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    if (nameIdx != -1) {
                        name = it.getString(nameIdx) ?: number
                    }
                    if (photoIdx != -1) {
                        val p = it.getString(photoIdx)
                        if (p != null) photoUri = Uri.parse(p)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        CallerInfo(name, photoUri)
    }

    private fun startTimer(initialSeconds: Long = 0L) {
        timerJob?.cancel()
        timerJob = scope.launch {
            var count = initialSeconds
            _callState.value = _callState.value.copy(durationSeconds = count)
            while (isActive) {
                delay(1000)
                count++
                _callState.value = _callState.value.copy(durationSeconds = count)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
