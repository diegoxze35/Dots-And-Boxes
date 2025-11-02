package com.mobile.dab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobile.dab.R

/*@Composable
fun HistoryDialog(history: List<GameResult>, onDismiss: () -> Unit) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("History") },
		text = {
			if (history.isEmpty()) {
				Text("No games yet")
			} else {
				Column {
					for (item in history) {
						Text(
							"${item.playerOne} vs ${item.playerTwo ?: "-"} — Winner: ${item.winner ?: "-"} — ${
								java.text.SimpleDateFormat.getDateTimeInstance()
									.format(java.util.Date(item.timestamp))
							}"
						)
						Spacer(modifier = Modifier.height(6.dp))
					}
				}
			}
		},
		confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
	)
}*/
