import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobile.dab.game.GameResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// This is the entity class you provided
/*
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
*/

/**
 * Displays a list of game results or a message if the list is empty.
 */
@Composable
fun HistoryScreen(results: List<GameResult>) {
    if (results.isEmpty()) {
        // Show empty state message
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "There are no records yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        // Show the list of results
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = results, key = { it.id }) { result ->
                HistoryItem(result = result)
            }
        }
    }
}

/**
 * A private composable function to display a single game result item.
 */
@Composable
private fun HistoryItem(result: GameResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Icon for game type (Human vs. Computer or Human vs. Human)
            val gameTypeIcon = if (result.isVsComputer) {
                Icons.Default.Computer
            } else {
                Icons.Default.People
            }
            Icon(
                imageVector = gameTypeIcon,
                contentDescription = if (result.isVsComputer) "Vs. Computer" else "Vs. Player",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(16.dp))

            // 2. Game Details Column
            Column(modifier = Modifier.weight(1f)) {
                // Players
                val opponent = if (result.isVsComputer) "Computer" else (result.playerTwo ?: "Player 2")
                Text(
                    text = "${result.playerOne} vs. $opponent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                // Winner
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Winner",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFA726) // A gold/yellow color for the trophy
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Winner: ${result.winner ?: "Draw"}", // Handle null winner as a Draw
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 3. Timestamp and Duration
                val formattedDate = formatTimestamp(result.timestamp)
                val formattedDuration = formatDuration(result.durationMs)
                Text(
                    text = "$formattedDate • $formattedDuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// --- Helper Functions ---

/**
 * Formats a Long timestamp into a human-readable date and time string.
 * Example: "Nov 01, 2025, 4:30 PM"
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Formats a Long duration in milliseconds into a "Xm Ys" string.
 * Example: "2m 15s"
 */
private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}