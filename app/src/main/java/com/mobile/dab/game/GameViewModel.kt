package com.mobile.dab.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

// UI state exposed to Compose
data class GameUiState(
    val gridRows: Int = 4, // dots rows
    val gridCols: Int = 4, // dots cols
    val placedLines: Set<Line> = emptySet(),
    val lineOwners: Map<Line, Int> = emptyMap(), // owner index for each placed line
    val boxOwners: Map<Box, Int> = emptyMap(),
    val players: List<Player> = listOf(Player(0, "Player 1", PlayerType.HUMAN), Player(1, "Player 2", PlayerType.COMPUTER)),
    val currentPlayerIndex: Int = 0,
    val scores: Map<Int, Int> = mapOf(0 to 0, 1 to 0),
    val isGameOver: Boolean = false,
    val winner: String? = null,
    val isVsComputer: Boolean = true
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = GameRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    // History of game results
    private val _history = MutableStateFlow<List<GameResult>>(emptyList())
    val history: StateFlow<List<GameResult>> = _history

    private var startTime: Long = 0L

    init {
        startNewGame(vsComputer = true)
        // load persisted history
        viewModelScope.launch { loadHistory() }
    }

    fun startNewGame(vsComputer: Boolean) {
        val defaultPlayers = if (vsComputer) listOf(Player(0, "You", PlayerType.HUMAN), Player(1, "Computer", PlayerType.COMPUTER))
        else listOf(Player(0, "Player 1", PlayerType.HUMAN), Player(1, "Player 2", PlayerType.HUMAN))
        _uiState.value = _uiState.value.copy(
            placedLines = emptySet(),
            lineOwners = emptyMap(),
            boxOwners = emptyMap(),
            scores = mapOf(0 to 0, 1 to 0),
            currentPlayerIndex = 0,
            players = defaultPlayers,
            isGameOver = false,
            winner = null,
            isVsComputer = vsComputer
        )
        startTime = System.currentTimeMillis()

        // If computer starts, make a move
        if (_uiState.value.players[_uiState.value.currentPlayerIndex].type == PlayerType.COMPUTER) {
            viewModelScope.launch {
                makeAIMoveIfNeeded()
            }
        }
    }

    fun toggleVsComputer() {
        startNewGame(vsComputer = !_uiState.value.isVsComputer)
    }

    fun makeMove(line: Line) {
        // Optimistic: reflect the move immediately in uiState so UI sees it right away
        val current = _uiState.value
        if (current.isGameOver) return
        if (line in current.placedLines) return

        // Apply optimistic placed line and owner (current player)
        val optimisticOwners = current.lineOwners + (line to current.currentPlayerIndex)
        _uiState.value = current.copy(placedLines = current.placedLines + line, lineOwners = optimisticOwners)
        android.util.Log.d("GameViewModel", "makeMove called optimistically for $line by player ${current.currentPlayerIndex}")

        // Continue processing game logic asynchronously
        viewModelScope.launch(Dispatchers.Default) {
            // Re-read the latest snapshot close to where we compute derived state to avoid stomping concurrent updates
            val latest = _uiState.value // includes optimistic change

            // Defensive: if the line is no longer in the latest placed set, it means something removed it or it was invalid
            if (line !in latest.placedLines) {
                android.util.Log.d("GameViewModel", "makeMove aborted: $line no longer present in placedLines")
                return@launch
            }

            // Compute completed boxes and scoring using the latest placed set
            val newPlaced = latest.placedLines

            val completed = checkCompletedBoxes(line, latest.gridRows, latest.gridCols, latest.boxOwners.keys, newPlaced)
            val mutableBoxOwners = latest.boxOwners.toMutableMap()
            val scoresMutable = latest.scores.toMutableMap()
            // ensure line owner is set (in case optimistic wasn't present)
            val mutableLineOwners = latest.lineOwners.toMutableMap()
            mutableLineOwners[line] = latest.currentPlayerIndex

            if (completed.isNotEmpty()) {
                for (b in completed) {
                    mutableBoxOwners[b] = latest.currentPlayerIndex
                }
                scoresMutable[latest.currentPlayerIndex] = (scoresMutable[latest.currentPlayerIndex] ?: 0) + completed.size
            }

            var nextPlayer = latest.currentPlayerIndex
            if (completed.isEmpty()) {
                nextPlayer = 1 - latest.currentPlayerIndex
            }

            val isGameOver = checkGameOver(latest.gridRows, latest.gridCols, newPlaced)
            val winner = if (isGameOver) determineWinner(scoresMutable, latest.players) else null

            // Publish final resolved state based on the most recent snapshot
            _uiState.value = latest.copy(
                placedLines = newPlaced,
                lineOwners = mutableLineOwners.toMap(),
                boxOwners = mutableBoxOwners.toMap(),
                scores = scoresMutable.toMap(),
                currentPlayerIndex = nextPlayer,
                isGameOver = isGameOver,
                winner = winner
            )

            android.util.Log.d("GameViewModel", "makeMove processed $line; completed=${completed.size}; next=$nextPlayer; isGameOver=$isGameOver")

            if (isGameOver) {
                val duration = System.currentTimeMillis() - startTime
                saveResultAsync(_uiState.value, duration)
            } else {
                // if next player is computer, trigger AI
                if (_uiState.value.players[_uiState.value.currentPlayerIndex].type == PlayerType.COMPUTER) {
                    makeAIMoveIfNeeded()
                }
            }
        }
    }

    private fun determineWinner(scores: Map<Int, Int>, players: List<Player>): String? {
        val p0 = scores[0] ?: 0
        val p1 = scores[1] ?: 0
        return when {
            p0 > p1 -> players[0].name
            p1 > p0 -> players[1].name
            else -> "Tie"
        }
    }

    private fun checkGameOver(gridRows: Int, gridCols: Int, placed: Set<Line>): Boolean {
        // total boxes = (rows-1)*(cols-1)
        val totalBoxes = (gridRows - 1) * (gridCols - 1)
        // count owned boxes by checking completed boxes from placed lines
        var completed = 0
        for (r in 0 until gridRows - 1) {
            for (c in 0 until gridCols - 1) {
                val box = Box(r, c)
                val top = Line(r, c, Orientation.HORIZONTAL)
                val bottom = Line(r + 1, c, Orientation.HORIZONTAL)
                val left = Line(r, c, Orientation.VERTICAL)
                val right = Line(r, c + 1, Orientation.VERTICAL)
                if (top in placed && bottom in placed && left in placed && right in placed) completed++
            }
        }
        return completed >= totalBoxes
    }

    private fun checkCompletedBoxes(line: Line, gridRows: Int, gridCols: Int, existingBoxes: Set<Box>, placedWithNew: Set<Line>): List<Box> {
        val boxes = mutableListOf<Box>()
        // check neighboring boxes of the placed line
        if (line.orientation == Orientation.HORIZONTAL) {
            // box above
            if (line.row - 1 >= 0) {
                val box = Box(line.row - 1, line.col)
                if (isBoxCompleted(box, placedWithNew)) boxes.add(box)
            }
            // box below
            if (line.row < gridRows - 1) {
                val box = Box(line.row, line.col)
                if (isBoxCompleted(box, placedWithNew)) boxes.add(box)
            }
        } else {
            // vertical -> left and right
            if (line.col - 1 >= 0) {
                val box = Box(line.row, line.col - 1)
                if (isBoxCompleted(box, placedWithNew)) boxes.add(box)
            }
            if (line.col < gridCols - 1) {
                val box = Box(line.row, line.col)
                if (isBoxCompleted(box, placedWithNew)) boxes.add(box)
            }
        }
        return boxes
    }

    private fun isBoxCompleted(box: Box, placed: Set<Line>): Boolean {
        val top = Line(box.row, box.col, Orientation.HORIZONTAL)
        val bottom = Line(box.row + 1, box.col, Orientation.HORIZONTAL)
        val left = Line(box.row, box.col, Orientation.VERTICAL)
        val right = Line(box.row, box.col + 1, Orientation.VERTICAL)
        return top in placed && bottom in placed && left in placed && right in placed
    }

    private fun saveResultAsync(state: GameUiState, duration: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val winnerName = state.winner
            val result = GameResult(
                playerOne = state.players[0].name,
                playerTwo = state.players.getOrNull(1)?.name,
                winner = winnerName,
                timestamp = System.currentTimeMillis(),
                durationMs = duration,
                isVsComputer = state.isVsComputer
            )
            repo.saveResult(result)
            // reload history after saving
            loadHistory()
        }
    }

    suspend fun loadHistory() {
        val list = repo.getHistory()
        _history.value = list
    }

    private fun makeAIMoveIfNeeded() {
        viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val move = AIHelper.chooseMove(state.gridRows, state.gridCols, state.placedLines)
            if (move != null) {
                // small delay to simulate thinking
                kotlinx.coroutines.delay(400)
                android.util.Log.d("GameViewModel", "AI chose move $move")
                makeMove(move)
            }
        }
    }
}
