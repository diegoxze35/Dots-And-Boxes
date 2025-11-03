package com.mobile.dab.game

import kotlinx.serialization.Serializable
import kotlin.system.measureTimeMillis

// Represents a player
enum class PlayerType { HUMAN, COMPUTER }

data class Player(
    val id: Int,
    val name: String,
    val type: PlayerType = PlayerType.HUMAN
)

// Orientation for a line between two dots
@Serializable
enum class Orientation { HORIZONTAL, VERTICAL }

// A line placed between two neighboring dots at row,col with orientation
@Serializable
data class Line(val row: Int, val col: Int, val orientation: Orientation)

// Box coordinate (top-left dot index)
@Serializable
data class Box(val row: Int, val col: Int)

// A move result
sealed class MoveResult {
    object Invalid : MoveResult()
    data class Placed(val completedBoxes: Int) : MoveResult()
}

