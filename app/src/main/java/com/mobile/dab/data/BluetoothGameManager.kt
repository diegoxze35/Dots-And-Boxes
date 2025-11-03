package com.mobile.dab.data

import com.mobile.dab.domain.ConnectionStatus
import com.mobile.dab.domain.Line
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object BluetoothGameManager {

    private val _connectionStatus = MutableStateFlow(
        ConnectionStatus.Idle
    )
    val connectionStatus = _connectionStatus.asStateFlow()
    private val _incomingMoves = MutableSharedFlow<Line>(extraBufferCapacity = 1)
    val incomingMoves = _incomingMoves.asSharedFlow()
    private val _outgoingMoves = MutableSharedFlow<Line>(extraBufferCapacity = 1)
    val outgoingMoves = _outgoingMoves.asSharedFlow()

    fun emitIncomingMove(line: Line) {
        _incomingMoves.tryEmit(line)
    }

    fun updateStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
    }

    fun resetConnectionStatus() {
        _connectionStatus.value = ConnectionStatus.Idle
    }

    fun emitOutgoingMove(line: Line) {
        _outgoingMoves.tryEmit(line)
    }

}