package com.mobile.dab.ui.composable.bluetooth.state

import android.bluetooth.BluetoothDevice
import com.mobile.dab.domain.ConnectionStatus

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Idle,
    val errorMessage: String? = null
)
