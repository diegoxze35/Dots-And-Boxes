package com.mobile.dab.ui.composable.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.dab.data.BluetoothGameManager
import com.mobile.dab.data.AIHelper
import com.mobile.dab.data.GameRepository
import com.mobile.dab.data.entity.GameResult
import com.mobile.dab.data.json.SavedGameState
import com.mobile.dab.domain.Box
import com.mobile.dab.domain.Line
import com.mobile.dab.domain.Orientation
import com.mobile.dab.domain.Player
import com.mobile.dab.domain.PlayerType
import com.mobile.dab.data.SavedGameRepository
import com.mobile.dab.ui.composable.game.state.GameUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = GameRepository(application.applicationContext)
    private val savedGameRepo = SavedGameRepository(application.applicationContext)
    private val _savedGames = MutableStateFlow<List<SavedGameState>>(emptyList())
    val savedGames: StateFlow<List<SavedGameState>> = _savedGames.asStateFlow()
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState
    private val _history = MutableStateFlow<List<GameResult>>(emptyList())
    val history: StateFlow<List<GameResult>> = _history
    private var startTime: Long = 0L
    private var isProcessingMove = false

    init {
        viewModelScope.launch {
            BluetoothGameManager.incomingMoves
                .onEach { line -> receiveMove(line) }
                .launchIn(this)
        }
    }

    fun startNewGame(vsComputer: Boolean) {
        val defaultPlayers = if (vsComputer) listOf(
            Player(0, "You", PlayerType.HUMAN),
            Player(1, "Computer", PlayerType.COMPUTER)
        )
        else listOf(
            Player(0, "Player 1", PlayerType.HUMAN),
            Player(1, "Player 2", PlayerType.HUMAN)
        )
        _uiState.value = GameUiState(
            players = defaultPlayers,
            isVsComputer = vsComputer,
            isBluetoothGame = false,
            localPlayerIndex = 0,
            isMyTurn = true
        )
        startTime = System.currentTimeMillis()

        if (vsComputer && _uiState.value.currentPlayerIndex == 1) {
            viewModelScope.launch { makeAIMoveIfNeeded() }
        }
    }

    fun startBluetoothGame(isServer: Boolean) {
        val localPlayerIdx = if (isServer) 0 else 1

        val players = if (isServer) listOf(
            Player(0, "You (Host)", PlayerType.HUMAN),
            Player(1, "Opponent (Client)", PlayerType.HUMAN)
        ) else listOf(
            Player(0, "Opponent (Host)", PlayerType.HUMAN),
            Player(1, "You (Client)", PlayerType.HUMAN)
        )

        _uiState.value = GameUiState(
            players = players,
            isVsComputer = false,
            isBluetoothGame = true,
            localPlayerIndex = localPlayerIdx,
            isMyTurn = (localPlayerIdx == 0)
        )
        startTime = System.currentTimeMillis()
    }

    fun makeMove(line: Line) {
        val current = _uiState.value
        if (isProcessingMove || current.isGameOver || line in current.placedLines) return
        if (current.isBluetoothGame && !current.isMyTurn) return
        processMoveLogic(line, isLocalMove = true)
    }

    private fun receiveMove(line: Line) {
        val current = _uiState.value
        if (!current.isBluetoothGame || isProcessingMove || current.isGameOver || line in current.placedLines) return
        processMoveLogic(line, isLocalMove = false)
    }

    private fun processMoveLogic(line: Line, isLocalMove: Boolean) {
        if (isProcessingMove) return
        isProcessingMove = true

        val current = _uiState.value
        val optimisticOwners = current.lineOwners + (line to current.currentPlayerIndex)
        _uiState.value =
            current.copy(placedLines = current.placedLines + line, lineOwners = optimisticOwners)

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val latest = _uiState.value

                if (line !in latest.placedLines) {
                    return@launch
                }

                val newPlaced = latest.placedLines
                val completed = checkCompletedBoxes(
                    line,
                    latest.gridRows,
                    latest.gridCols,
                    newPlaced
                )
                val mutableBoxOwners = latest.boxOwners.toMutableMap()
                val scoresMutable = latest.scores.toMutableMap()
                val mutableLineOwners = latest.lineOwners.toMutableMap()
                mutableLineOwners[line] = latest.currentPlayerIndex

                if (completed.isNotEmpty()) {
                    for (b in completed) {
                        mutableBoxOwners[b] = latest.currentPlayerIndex
                    }
                    scoresMutable[latest.currentPlayerIndex] =
                        (scoresMutable[latest.currentPlayerIndex] ?: 0) + completed.size
                }

                var nextPlayer = latest.currentPlayerIndex
                if (completed.isEmpty()) {
                    nextPlayer = 1 - latest.currentPlayerIndex
                }

                val isGameOver = checkGameOver(latest.gridRows, latest.gridCols, newPlaced)
                val winner = if (isGameOver) determineWinner(scoresMutable, latest.players) else null

                _uiState.value = latest.copy(
                    placedLines = newPlaced,
                    lineOwners = mutableLineOwners.toMap(),
                    boxOwners = mutableBoxOwners.toMap(),
                    scores = scoresMutable.toMap(),
                    currentPlayerIndex = nextPlayer,
                    isGameOver = isGameOver,
                    winner = winner,
                    isMyTurn = (nextPlayer == latest.localPlayerIndex) || !latest.isBluetoothGame
                )

                if (latest.isBluetoothGame && isLocalMove) {
                    launch(Dispatchers.IO) {
                        BluetoothGameManager.emitOutgoingMove(line)
                    }
                }

                if (isGameOver) {
                    val duration = System.currentTimeMillis() - startTime
                    val currentState = _uiState.value
                    if (!currentState.isBluetoothGame || (currentState.isBluetoothGame && currentState.localPlayerIndex == 0)) {
                        saveResultAsync(currentState, duration)
                    }
                } else {
                    if (!latest.isBluetoothGame && _uiState.value.players[_uiState.value.currentPlayerIndex].type == PlayerType.COMPUTER) {
                        makeAIMoveIfNeeded()
                    }
                }
            } finally {
                isProcessingMove = false
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
        val totalBoxes = (gridRows - 1) * (gridCols - 1)
        var completed = 0
        for (r in 0 until gridRows - 1) {
            for (c in 0 until gridCols - 1) {
                val box = Box(r, c)
                if (isBoxCompleted(box, placed)) completed++
            }
        }
        return completed >= totalBoxes
    }

    private fun checkCompletedBoxes(
        line: Line,
        gridRows: Int,
        gridCols: Int,
        placedWithNew: Set<Line>
    ): List<Box> {
        val boxes = mutableListOf<Box>()
        if (line.orientation == Orientation.HORIZONTAL) {
            if (line.row - 1 >= 0) {
                val box = Box(line.row - 1, line.col)
                if (isBoxCompleted(box, placedWithNew)) boxes.add(box)
            }
            if (line.row < gridRows - 1) {
                val box = Box(line.row, line.col)
                if (isBoxCompleted(box, placedWithNew)) boxes.add(box)
            }
        } else {
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
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            val list = repo.getHistory()
            _history.value = list
        }
    }

    private fun makeAIMoveIfNeeded() {
        viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val move = AIHelper.chooseMove(state.gridRows, state.gridCols, state.placedLines)
            if (move != null) {
                delay(400)
                processMoveLogic(move, isLocalMove = false)
            }
        }
    }
    fun saveCurrentGame() {
        viewModelScope.launch(Dispatchers.IO) {
            val stateToSave = _uiState.value
            if (stateToSave.isBluetoothGame) return@launch
            savedGameRepo.saveGame(stateToSave, startTime)
            loadSavedGames()
        }
    }

    fun loadSavedGames() {
        viewModelScope.launch(Dispatchers.IO) {
            _savedGames.value = savedGameRepo.getSavedGames()
        }
    }

    fun loadGame(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedGameState = savedGameRepo.loadGame(fileName)
            if (savedGameState != null) {
                launch(Dispatchers.Main) {
                    _uiState.value = savedGameState.gameState
                    startTime = savedGameState.startTime
                }
            }
        }
    }

    fun deleteSavedGame(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            savedGameRepo.deleteGame(fileName)
            loadSavedGames()
        }
    }

}
