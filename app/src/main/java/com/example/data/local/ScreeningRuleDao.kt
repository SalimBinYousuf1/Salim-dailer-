package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreeningRuleDao {
    @Query("SELECT * FROM screening_rules")
    fun getAllRules(): Flow<List<ScreeningRuleEntity>>

    @Query("SELECT enabled FROM screening_rules WHERE id = :id LIMIT 1")
    suspend fun isRuleEnabled(id: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRule(rule: ScreeningRuleEntity)

    @Query("SELECT * FROM quick_decline_messages ORDER BY id ASC")
    fun getAllQuickMessages(): Flow<List<QuickDeclineMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickMessage(message: QuickDeclineMessageEntity)

    @Query("DELETE FROM quick_decline_messages WHERE id = :id")
    suspend fun deleteQuickMessage(id: Long)
}
