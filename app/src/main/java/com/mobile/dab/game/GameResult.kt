package com.mobile.dab.game

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerOne: String,
    val playerTwo: String?,
    val winner: String?,
    val timestamp: Long,
    val durationMs: Long,
    val isVsComputer: Boolean
)

