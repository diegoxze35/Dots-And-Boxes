package com.mobile.dab.ui.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mobile.dab.R
import com.mobile.dab.game.GameUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopGameAppBar(
	modifier: Modifier = Modifier,
	onClickNavigationIcon: () -> Unit,
	state: GameUiState
) {
	TopAppBar(
		modifier = modifier,
		title = {
			Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
		},
		navigationIcon = {
			IconButton(onClick = onClickNavigationIcon) {
				Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
			}
		}
	)
	/*Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		Row {
			TextButton(onClick = onShowHistory) { Text("History") }
			Button(onClick = onToggleMode, shape = RoundedCornerShape(8.dp)) {
				Text(if (state.isVsComputer) "Vs Computer" else "Local")
			}
			Spacer(modifier = Modifier.width(8.dp))
			Button(onClick = onNewGame, shape = RoundedCornerShape(8.dp)) { Text("New Game") }
		}
	}*/
}
