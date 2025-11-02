package com.mobile.dab.game

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GameResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(result: GameResult): Long

    @Query("SELECT * FROM game_results ORDER BY timestamp DESC")
    fun getAll(): List<GameResult>
}
