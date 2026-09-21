package com.example.data.model

import android.net.Uri

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED,
    VOICEMAIL,
    UNKNOWN
}

data class CallLogEntry(
    val id: Long,
    val number: String,
    val formattedNumber: String,
    val name: String?,
    val photoUri: Uri?,
    val type: CallType,
    val timestamp: Long,
    val durationSeconds: Long,
    val subscriptionId: Int = -1,
    val count: Int = 1,
    val callIds: List<Long> = listOf(id)
)
