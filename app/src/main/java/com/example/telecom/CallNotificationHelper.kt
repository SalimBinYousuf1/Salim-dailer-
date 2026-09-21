package com.example.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.model.CallStateInfo
import com.example.ui.incall.InCallActivity

object CallNotificationHelper {

    const val CHANNEL_INCOMING = "salim_incoming_calls"
    const val CHANNEL_ONGOING = "salim_ongoing_calls"
    const val NOTIFICATION_ID_CALL = 1001

    const val ACTION_ANSWER = "com.example.ACTION_ANSWER"
    const val ACTION_DECLINE = "com.example.ACTION_DECLINE"
    const val ACTION_MUTE_TOGGLE = "com.example.ACTION_MUTE_TOGGLE"
    const val ACTION_SPEAKER_TOGGLE = "com.example.ACTION_SPEAKER_TOGGLE"
    const val ACTION_HANG_UP = "com.example.ACTION_HANG_UP"

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Incoming calls channel
        val incomingChannel = NotificationChannel(
            CHANNEL_INCOMING,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for incoming phone calls"
            enableLights(true)
            enableVibration(true)
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            setSound(ringtoneUri, audioAttributes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // Ongoing calls channel
        val ongoingChannel = NotificationChannel(
            CHANNEL_ONGOING,
            "Ongoing Calls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Status for active phone calls"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(incomingChannel)
        manager.createNotificationChannel(ongoingChannel)
    }

    fun buildIncomingCallNotification(context: Context, callInfo: CallStateInfo): Notification {
        val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("is_incoming", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            1,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(context, SalimInCallService::class.java).apply {
            action = ACTION_ANSWER
        }
        val answerPendingIntent = PendingIntent.getService(
            context,
            2,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, SalimInCallService::class.java).apply {
            action = ACTION_DECLINE
        }
        val declinePendingIntent = PendingIntent.getService(
            context,
            3,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (callInfo.displayName.isNotBlank()) callInfo.displayName else callInfo.number
        val contentText = if (callInfo.displayName.isNotBlank()) callInfo.number else "Incoming Call"

        return NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_call, "Answer", answerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePendingIntent)
            .build()
    }

    fun buildOngoingCallNotification(context: Context, callInfo: CallStateInfo): Notification {
        val openIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            4,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hangUpIntent = Intent(context, SalimInCallService::class.java).apply {
            action = ACTION_HANG_UP
        }
        val hangUpPendingIntent = PendingIntent.getService(
            context,
            5,
            hangUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (callInfo.displayName.isNotBlank()) callInfo.displayName else callInfo.number
        val seconds = callInfo.durationSeconds
        val formattedTime = String.format("%02d:%02d", seconds / 60, seconds % 60)
        val contentText = "In call • $formattedTime"

        return NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Call", hangUpPendingIntent)
            .build()
    }
}
