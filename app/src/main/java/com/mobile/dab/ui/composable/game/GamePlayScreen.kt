package com.mobile.dab.ui.composable.game

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobile.dab.R
import com.mobile.dab.domain.Line
import com.mobile.dab.ui.composable.game.state.GameUiState

@Composable
fun GamePlayScreen(
	modifier: Modifier = Modifier,
	state: GameUiState,
	onLineSelected: (Line) -> Unit,
	onSaveGame: () -> Unit
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
                isInteractionEnabled = state.isMyTurn,
				onDebug = { msg -> Log.i("GamePlayScreen", msg) }
			)
		}

        if (!state.isBluetoothGame && !state.isGameOver) {
            Button(
                onClick = onSaveGame,
                modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
            ) {
                Text(stringResource(R.string.save_game))
            }
        }
		Spacer(modifier = Modifier.height(8.dp))
        if (state.isGameOver) {
			Text(
				text = stringResource(R.string.game_over_winner, state.winner!!),
				fontWeight = FontWeight.Bold
			)
		} else if (state.isBluetoothGame && !state.isMyTurn) {
            Text(
                text = stringResource(R.string.waiting_for_opponent),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
	}
}
