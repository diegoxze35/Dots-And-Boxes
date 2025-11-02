package com.mobile.dab.ui.composable.game

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobile.dab.game.GameUiState
import com.mobile.dab.game.Line

@Composable
fun GamePlayScreen(
	modifier: Modifier = Modifier,
	state: GameUiState,
	onLineSelected: (Line) -> Unit,
) {
	
	Column(modifier = modifier) {
		Spacer(modifier = Modifier.height(8.dp))
		ScoreBoard(state = state)
		Spacer(modifier = Modifier.height(8.dp))
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f),
			contentAlignment = Alignment.Center
		) {
			Board(
				modifier = Modifier.padding(horizontal = 8.dp),
				gridRows = state.gridRows,
				gridCols = state.gridCols,
				state = state,
				onLineSelected = onLineSelected,
				onDebug = { msg -> Log.i("GamePlayScreen", msg) }
			)
		}
		
		Spacer(modifier = Modifier.height(8.dp))
		
		if (state.isGameOver) {
			Text(
				text = "Game Over! Winner: ${state.winner}",
				fontWeight = FontWeight.Bold
			)
		}
	}
	
	/*if (showHistory) {
		HistoryDialog(history = history, onDismiss = { showHistory = false })
	}*/
}
