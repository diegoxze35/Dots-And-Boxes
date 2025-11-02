package com.mobile.dab.ui.composable.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mobile.dab.game.GameUiState

@Composable
internal fun ScoreBoard(state: GameUiState) {
	Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
		val p0 = state.players.getOrNull(0)?.name ?: "P1"
		val p1 = state.players.getOrNull(1)?.name ?: "P2"
		val score0 = state.scores[0] ?: 0
		val score1 = state.scores[1] ?: 0
		
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(text = p0)
			Text(text = score0.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
			if (state.currentPlayerIndex == 0) Text(
				"Your Turn",
				color = MaterialTheme.colorScheme.primary
			)
		}
		
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(text = p1)
			Text(text = score1.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
			if (state.currentPlayerIndex == 1) Text(
				"Their Turn",
				color = MaterialTheme.colorScheme.secondary
			)
		}
	}
}
