package com.mobile.dab.ui.composable.bluetooth.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.dab.data.BluetoothGameManager
import com.mobile.dab.domain.ConnectionStatus
import com.mobile.dab.domain.Line
import com.mobile.dab.ui.composable.bluetooth.state.BluetoothUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

private const val SERVICE_NAME = "DotsAndBoxesBT"
private val SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

@SuppressLint("MissingPermission")
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager by lazy {
        application.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    private var isServer: Boolean = false
    val amIServer: Boolean get() = isServer

    private var dataTransferJob: Job? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    init {
        BluetoothGameManager.outgoingMoves
            .onEach { line -> sendMove(line) }
            .launchIn(viewModelScope)
    }

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                    device?.let {
                        if (!_uiState.value.scannedDevices.any { d -> d.address == it.address }) {
                            _uiState.update { state ->
                                state.copy(scannedDevices = state.scannedDevices + it)
                            }
                        }
                    }
                }
            }
        }
    }

    fun startScan() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN)) return
        }
        _uiState.update { it.copy(isScanning = true, scannedDevices = emptyList()) }
        bluetoothAdapter?.bondedDevices?.let { bondedDevices ->
            _uiState.update { state ->
                state.copy(scannedDevices = bondedDevices.toList())
            }
        }
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
        getApplication<Application>().registerReceiver(
            scanReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        bluetoothAdapter?.startDiscovery()
    }

    fun stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN)) return
        }
        bluetoothAdapter?.cancelDiscovery()
        _uiState.update { it.copy(isScanning = false) }
        try {
            getApplication<Application>().unregisterReceiver(scanReceiver)
        } catch (e: Exception) {
            Log.e("BluetoothViewModel", "Receiver not registered", e)
        }
    }

    fun startServer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN)) return
        }
        isServer = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                serverSocket =
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Waiting) }
                BluetoothGameManager.updateStatus(ConnectionStatus.Waiting)
                val socket = serverSocket?.accept()
                socket?.let {
                    serverSocket?.close()
                    clientSocket = it
                    outputStream = it.outputStream
                    inputStream = it.inputStream
                    _uiState.update { s -> s.copy(connectionStatus = ConnectionStatus.Connected) }
                    BluetoothGameManager.updateStatus(ConnectionStatus.Connected)
                    startDataListener()
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.Idle,
                        errorMessage = e.message
                    )
                }
                BluetoothGameManager.updateStatus(ConnectionStatus.Idle)
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN)) return
        }
        isServer = false
        stopScan()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connecting) }
                BluetoothGameManager.updateStatus(ConnectionStatus.Connecting)
                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()
                clientSocket = socket
                outputStream = socket.outputStream
                inputStream = socket.inputStream
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connected) }
                BluetoothGameManager.updateStatus(ConnectionStatus.Connected)
                startDataListener()
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.Idle,
                        errorMessage = e.message
                    )
                }
                BluetoothGameManager.updateStatus(ConnectionStatus.Idle)
            }
        }
    }

    private fun startDataListener() {
        dataTransferJob = viewModelScope.launch(Dispatchers.IO) {
            val reader = inputStream?.bufferedReader()
            try {
                while (true) {
                    val lineJson = reader?.readLine()
                    if (lineJson == null) {
                        break
                    }

                    try {
                        val line = Json.decodeFromString<Line>(lineJson)
                        BluetoothGameManager.emitIncomingMove(line)
                    } catch (e: Exception) {
                        Log.e("BluetoothViewModel", "Error decoding JSON: $lineJson", e)
                    }
                }
            } catch (e: IOException) {
                Log.e("BluetoothViewModel", "Error reading data, connection closed", e)
            } finally {
                disconnect()
            }
        }
    }

    private fun sendMove(line: Line) {
        if (clientSocket?.isConnected != true) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lineJson = Json.encodeToString(line)
                outputStream?.write((lineJson + "\n").toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                Log.e("BluetoothViewModel", "Error sending move", e)
                disconnect()
            }
        }
    }

    fun disconnect() {
        try {
            dataTransferJob?.cancel()
            serverSocket?.close()
            clientSocket?.close()
            inputStream?.close()
            outputStream?.close()
        } catch (e: IOException) {
            Log.e("BluetoothViewModel", "Error disconnecting", e)
        }
        _uiState.update { BluetoothUiState() }
        BluetoothGameManager.updateStatus(ConnectionStatus.Idle)
    }

    override fun onCleared() {
        stopScan()
        disconnect()
        super.onCleared()
    }

    private fun hasPermission(permission: String): Boolean {
        return getApplication<Application>().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

}
