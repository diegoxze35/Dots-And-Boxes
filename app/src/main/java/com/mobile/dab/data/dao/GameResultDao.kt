package com.mobile.dab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.dab.data.entity.GameResult

@Dao
interface GameResultDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(result: GameResult): Long

    @Query("SELECT * FROM game_results ORDER BY timestamp DESC")
    fun getAll(): List<GameResult>
}