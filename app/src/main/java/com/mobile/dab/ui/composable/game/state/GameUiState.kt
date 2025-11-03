package com.mobile.dab.ui.composable.game.state

import com.mobile.dab.domain.Box
import com.mobile.dab.domain.Line
import com.mobile.dab.domain.Player
import com.mobile.dab.domain.PlayerType
import kotlinx.serialization.Serializable

@Serializable
data class GameUiState(
    val gridRows: Int = 4,
    val gridCols: Int = 4,
    val placedLines: Set<Line> = emptySet(),
    val lineOwners: Map<Line, Int> = emptyMap(),
    val boxOwners: Map<Box, Int> = emptyMap(),
    val players: List<Player> = listOf(
        Player(0, "Player 1", PlayerType.HUMAN),
        Player(1, "Player 2", PlayerType.COMPUTER)
    ),
    val currentPlayerIndex: Int = 0,
    val scores: Map<Int, Int> = mapOf(0 to 0, 1 to 0),
    val isGameOver: Boolean = false,
    val winner: String? = null,
    val isVsComputer: Boolean = true,
    val localPlayerIndex: Int = 0,
    val isMyTurn: Boolean = true,
    val isBluetoothGame: Boolean = false
)