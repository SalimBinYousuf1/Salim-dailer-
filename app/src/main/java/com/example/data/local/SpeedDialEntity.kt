package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_dial")
data class SpeedDialEntity(
    @PrimaryKey val digit: Int, // 1 to 9
    val contactName: String,
    val phoneNumber: String
)
