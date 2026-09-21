package com.example.data.repository

import com.example.data.local.SpeedDialDao
import com.example.data.local.SpeedDialEntity
import com.example.data.model.SpeedDialEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SpeedDialRepository(private val dao: SpeedDialDao) {

    val speedDialsFlow: Flow<List<SpeedDialEntry>> = dao.getAllSpeedDials().map { list ->
        list.map { entity ->
            SpeedDialEntry(
                key = entity.digit,
                name = entity.contactName,
                number = entity.phoneNumber
            )
        }
    }

    suspend fun getSpeedDial(digit: Int): SpeedDialEntry? {
        val entity = dao.getSpeedDial(digit) ?: return null
        return SpeedDialEntry(key = entity.digit, name = entity.contactName, number = entity.phoneNumber)
    }

    suspend fun setSpeedDial(digit: Int, name: String, number: String) {
        dao.setSpeedDial(SpeedDialEntity(digit = digit, contactName = name, phoneNumber = number))
    }

    suspend fun removeSpeedDial(digit: Int) {
        dao.removeSpeedDial(digit)
    }
}
