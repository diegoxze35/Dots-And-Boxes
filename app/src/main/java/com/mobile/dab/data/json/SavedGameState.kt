package com.mobile.dab.data.json

import com.mobile.dab.ui.composable.game.state.GameUiState
import kotlinx.serialization.Serializable

@Serializable
data class SavedGameState(
    val fileName: String,
    val gameState: GameUiState,
    val timestamp: Long,
    val startTime: Long
)
