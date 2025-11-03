package com.mobile.dab.ui.composable.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobile.dab.R
import com.mobile.dab.data.json.SavedGameState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedGamesScreen(
    games: List<SavedGameState>,
    onLoadGame: (String) -> Unit,
    onDeleteGame: (String) -> Unit
) {
    if (games.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_saved_games),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = games, key = { it.fileName }) { game ->
                SavedGameItem(
                    game = game,
                    onLoad = { onLoadGame(game.fileName) },
                    onDelete = { onDeleteGame(game.fileName) }
                )
            }
        }
    }
}

@Composable
private fun SavedGameItem(
    game: SavedGameState,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val state = game.gameState
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon
                val gameTypeIcon = if (state.isVsComputer) {
                    Icons.Default.Computer
                } else {
                    Icons.Default.People
                }
                Icon(
                    imageVector = gameTypeIcon,
                    contentDescription = if (state.isVsComputer) stringResource(R.string.vs_computer)
                    else stringResource(R.string.vs_player),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(16.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    val p1 = state.players.getOrNull(0)?.name ?: "P1"
                    val p2 = state.players.getOrNull(1)?.name ?: "P2"
                    Text(
                        text = "$p1 vs $p2",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.score, state.scores[0]!!, state.scores[1]!!),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatTimestamp(game.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onLoad,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.load_game))
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
    return "Saved: ${sdf.format(Date(timestamp))}"
}
