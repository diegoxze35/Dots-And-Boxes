package com.mobile.dab.bluetooth

import com.mobile.dab.game.Line
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object BluetoothGameManager {

    // 1. Estado de la conexión (para que la Activity sepa cuándo navegar)
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Idle)
    val connectionStatus = _connectionStatus.asStateFlow()

    // 2. Movimientos: Del BluetoothViewModel -> Al GameViewModel
    private val _incomingMoves = MutableSharedFlow<Line>(extraBufferCapacity = 1)
    val incomingMoves = _incomingMoves.asSharedFlow()

    // 3. Movimientos: Del GameViewModel -> Al BluetoothViewModel
    private val _outgoingMoves = MutableSharedFlow<Line>(extraBufferCapacity = 1)
    val outgoingMoves = _outgoingMoves.asSharedFlow()

    // --- Funciones para el BluetoothViewModel ---

    fun emitIncomingMove(line: Line) {
        _incomingMoves.tryEmit(line)
    }

    fun updateStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
    }

    fun resetConnectionStatus() {
        _connectionStatus.value = ConnectionStatus.Idle
    }

    // --- Funciones para el GameViewModel ---

    fun emitOutgoingMove(line: Line) {
        _outgoingMoves.tryEmit(line)
    }

}
