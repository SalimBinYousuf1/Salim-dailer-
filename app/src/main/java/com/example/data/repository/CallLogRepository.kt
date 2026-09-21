package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.telephony.PhoneNumberUtils
import com.example.data.model.CallLogEntry
import com.example.data.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class CallLogRepository(private val context: Context) {

    suspend fun getCallLogs(filterMissedOnly: Boolean = false): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val rawEntries = mutableListOf<CallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        val selection = if (filterMissedOnly) {
            "${CallLog.Calls.TYPE} = ?"
        } else null

        val selectionArgs = if (filterMissedOnly) {
            arrayOf(CallLog.Calls.MISSED_TYPE.toString())
        } else null

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val photoIdx = it.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val number = if (numberIdx != -1) it.getString(numberIdx) ?: "" else ""
                    val cachedName = if (nameIdx != -1) it.getString(nameIdx) else null
                    val photoUriStr = if (photoIdx != -1) it.getString(photoIdx) else null
                    val rawType = if (typeIdx != -1) it.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                    val duration = if (durationIdx != -1) it.getLong(durationIdx) else 0L

                    val callType = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                        CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
                        CallLog.Calls.VOICEMAIL_TYPE -> CallType.VOICEMAIL
                        else -> CallType.UNKNOWN
                    }

                    val formatted = try {
                        PhoneNumberUtils.formatNumber(number, Locale.getDefault().country) ?: number
                    } catch (e: Exception) {
                        number
                    }

                    rawEntries.add(
                        CallLogEntry(
                            id = id,
                            number = number,
                            formattedNumber = formatted,
                            name = cachedName,
                            photoUri = photoUriStr?.let { u -> Uri.parse(u) },
                            type = callType,
                            timestamp = date,
                            durationSeconds = duration
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        groupConsecutiveCalls(rawEntries)
    }

    private fun groupConsecutiveCalls(entries: List<CallLogEntry>): List<CallLogEntry> {
        if (entries.isEmpty()) return emptyList()

        val grouped = mutableListOf<CallLogEntry>()
        var currentGroup = entries.first()
        var currentCount = 1
        val currentIds = mutableListOf(currentGroup.id)

        for (i in 1 until entries.size) {
            val entry = entries[i]
            // Group if same number and same call type
            if (entry.number == currentGroup.number && entry.type == currentGroup.type) {
                currentCount++
                currentIds.add(entry.id)
            } else {
                grouped.add(
                    currentGroup.copy(
                        count = currentCount,
                        callIds = currentIds.toList()
                    )
                )
                currentGroup = entry
                currentCount = 1
                currentIds.clear()
                currentIds.add(entry.id)
            }
        }
        grouped.add(
            currentGroup.copy(
                count = currentCount,
                callIds = currentIds.toList()
            )
        )

        return grouped
    }

    suspend fun deleteCall(callIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        try {
            if (callIds.isEmpty()) return@withContext false
            val inClause = callIds.joinToString(",") { it.toString() }
            val deleted = context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} IN ($inClause)",
                null
            )
            deleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun clearCallLog(): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleted = context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                null,
                null
            )
            deleted >= 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
