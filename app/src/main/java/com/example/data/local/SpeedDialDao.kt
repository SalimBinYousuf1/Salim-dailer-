package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedDialDao {
    @Query("SELECT * FROM speed_dial ORDER BY digit ASC")
    fun getAllSpeedDials(): Flow<List<SpeedDialEntity>>

    @Query("SELECT * FROM speed_dial WHERE digit = :digit LIMIT 1")
    suspend fun getSpeedDial(digit: Int): SpeedDialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSpeedDial(speedDial: SpeedDialEntity)

    @Query("DELETE FROM speed_dial WHERE digit = :digit")
    suspend fun removeSpeedDial(digit: Int)
}
