package com.example.telecom

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.example.data.model.SalimCallState
import com.example.ui.incall.InCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SalimInCallService : InCallService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        CallManager.init(this)
        CallManager.attachService(this)

        CallManager.callState.onEach { state ->
            when (state.state) {
                SalimCallState.RINGING -> {
                    val notif = CallNotificationHelper.buildIncomingCallNotification(this, state)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            CallNotificationHelper.NOTIFICATION_ID_CALL,
                            notif,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                        )
                    } else {
                        startForeground(CallNotificationHelper.NOTIFICATION_ID_CALL, notif)
                    }
                }
                SalimCallState.ACTIVE, SalimCallState.DIALING, SalimCallState.CONNECTING, SalimCallState.HOLDING -> {
                    val notif = CallNotificationHelper.buildOngoingCallNotification(this, state)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            CallNotificationHelper.NOTIFICATION_ID_CALL,
                            notif,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                        )
                    } else {
                        startForeground(CallNotificationHelper.NOTIFICATION_ID_CALL, notif)
                    }
                }
                SalimCallState.DISCONNECTED, SalimCallState.IDLE -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm?.cancel(CallNotificationHelper.NOTIFICATION_ID_CALL)
                }
            }
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            CallNotificationHelper.ACTION_ANSWER -> {
                CallManager.answer()
                launchInCallActivity()
            }
            CallNotificationHelper.ACTION_DECLINE -> {
                CallManager.reject()
            }
            CallNotificationHelper.ACTION_HANG_UP -> {
                CallManager.disconnect()
            }
            CallNotificationHelper.ACTION_MUTE_TOGGLE -> {
                CallManager.toggleMute()
            }
            CallNotificationHelper.ACTION_SPEAKER_TOGGLE -> {
                CallManager.toggleSpeaker()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        call?.let {
            CallManager.registerCall(it)
            launchInCallActivity()
        }
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        call?.let {
            CallManager.unregisterCall(it)
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let {
            CallManager.onAudioStateChanged(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.detachService()
    }

    private fun launchInCallActivity() {
        val intent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }
}
