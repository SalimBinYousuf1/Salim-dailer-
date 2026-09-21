package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screening_rules")
data class ScreeningRuleEntity(
    @PrimaryKey val id: String, // "block_unknown", "block_private"
    val enabled: Boolean
)

@Entity(tableName = "quick_decline_messages")
data class QuickDeclineMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String
)
