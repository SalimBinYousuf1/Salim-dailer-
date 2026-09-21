package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BlockedNumberContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BlockedNumber(
    val id: Long,
    val number: String,
    val originalNumber: String
)

class BlockedNumberRepository(private val context: Context) {

    fun canBlockNumbers(): Boolean {
        return try {
            BlockedNumberContract.canCurrentUserBlockNumbers(context)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isNumberBlocked(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        if (phoneNumber.isBlank()) return@withContext false
        try {
            BlockedNumberContract.isBlocked(context, phoneNumber)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getBlockedNumbers(): List<BlockedNumber> = withContext(Dispatchers.IO) {
        val list = mutableListOf<BlockedNumber>()
        try {
            val cursor = context.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(
                    BlockedNumberContract.BlockedNumbers.COLUMN_ID,
                    BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                    BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER
                ),
                null,
                null,
                null
            )
            cursor?.use {
                val idIndex = it.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ID)
                val numberIndex = it.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
                val e164Index = it.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER)
                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getLong(idIndex) else 0L
                    val num = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                    val e164 = if (e164Index != -1) it.getString(e164Index) ?: num else num
                    list.add(BlockedNumber(id = id, number = num, originalNumber = e164))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun blockNumber(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        if (phoneNumber.isBlank()) return@withContext false
        try {
            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, phoneNumber)
            }
            val uri: Uri? = context.contentResolver.insert(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                values
            )
            uri != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun unblockNumber(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        if (phoneNumber.isBlank()) return@withContext false
        try {
            val count = BlockedNumberContract.unblock(context, phoneNumber)
            count > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
