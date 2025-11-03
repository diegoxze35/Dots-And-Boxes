package com.mobile.dab.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
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
import com.mobile.dab.game.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn // Importar
import kotlinx.coroutines.flow.onEach // Importar
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

// (Estado de la UI y ConnectionStatus no cambian)
data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Idle,
    val errorMessage: String? = null
)
enum class ConnectionStatus { Idle, Waiting, Connecting, Connected }

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

    // --- ELIMINADO: Ya no necesita su propio SharedFlow ---
    // private val _incomingMove = MutableSharedFlow<Line>()
    // val incomingMove: SharedFlow<Line> = _incomingMove.asSharedFlow()

    private var isServer: Boolean = false
    val amIServer: Boolean get() = isServer

    private var dataTransferJob: Job? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    // --- NUEVO: Escuchar movimientos salientes del GameManager ---
    init {
        BluetoothGameManager.outgoingMoves
            .onEach { line -> sendMove(line) }
            .launchIn(viewModelScope)
    }
    // --- FIN DEL NUEVO CÓDIGO ---

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
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                    device?.let {
                        // (Solución P1): No filtrar dispositivos sin nombre
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
        // (Comprobaciones de permisos - sin cambios)
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN)) return
        }

        // --- INICIO SOLUCIÓN P1 (Descubrimiento) ---
        // Limpiar lista anterior
        _uiState.update { it.copy(isScanning = true, scannedDevices = emptyList()) }

        // Añadir dispositivos YA EMPAREJADOS (muy importante)
        bluetoothAdapter?.bondedDevices?.let { bondedDevices ->
            _uiState.update { state ->
                state.copy(scannedDevices = bondedDevices.toList())
            }
        }
        // --- FIN SOLUCIÓN P1 ---

        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }

        getApplication<Application>().registerReceiver(
            scanReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        bluetoothAdapter?.startDiscovery()
        Log.d("BluetoothViewModel", "Iniciando escaneo (incluye emparejados)...")
    }

    fun stopScan() {
        // (Comprobaciones de permisos - sin cambios)
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
        Log.d("BluetoothViewModel", "Deteniendo escaneo.")
    }

    fun startServer() {
        // (Comprobaciones de permisos - sin cambios)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN)) return
        }

        isServer = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("BluetoothViewModel", "Iniciando servidor...")
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)

                // Actualizar UI y GameManager
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Waiting) }
                BluetoothGameManager.updateStatus(ConnectionStatus.Waiting)

                val socket = serverSocket?.accept()

                socket?.let {
                    Log.d("BluetoothViewModel", "Cliente conectado")
                    serverSocket?.close()
                    clientSocket = it
                    outputStream = it.outputStream
                    inputStream = it.inputStream

                    // Actualizar UI y GameManager
                    _uiState.update { s -> s.copy(connectionStatus = ConnectionStatus.Connected) }
                    BluetoothGameManager.updateStatus(ConnectionStatus.Connected)

                    startDataListener()
                }
            } catch (e: IOException) {
                Log.e("BluetoothViewModel", "Error al iniciar servidor", e)
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Idle, errorMessage = e.message) }
                BluetoothGameManager.updateStatus(ConnectionStatus.Idle)
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        // (Comprobaciones de permisos - sin cambios)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN)) return
        }

        isServer = false
        stopScan()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Actualizar UI y GameManager
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connecting) }
                BluetoothGameManager.updateStatus(ConnectionStatus.Connecting)

                Log.d("BluetoothViewModel", "Conectando a ${device.name}...")

                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()

                Log.d("BluetoothViewModel", "Conectado al servidor")
                clientSocket = socket
                outputStream = socket.outputStream
                inputStream = socket.inputStream

                // Actualizar UI y GameManager
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connected) }
                BluetoothGameManager.updateStatus(ConnectionStatus.Connected)

                startDataListener()

            } catch (e: IOException) {
                Log.e("BluetoothViewModel", "Error al conectar", e)
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Idle, errorMessage = e.message) }
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
                        Log.d("BluetoothViewModel", "Stream cerrado, desconectando.")
                        break
                    }

                    try {
                        val line = Json.decodeFromString<Line>(lineJson)
                        // --- CAMBIO: Usar el GameManager ---
                        BluetoothGameManager.emitIncomingMove(line)
                    } catch (e: Exception) {
                        Log.e("BluetoothViewModel", "Error al decodificar JSON: $lineJson", e)
                    }
                }
            } catch (e: IOException) {
                Log.e("BluetoothViewModel", "Error al leer datos, conexión perdida", e)
            } finally {
                disconnect()
            }
        }
    }

    // Esta función AHORA es llamada por el Colector de 'outgoingMoves'
    private fun sendMove(line: Line) {
        if (clientSocket?.isConnected != true) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lineJson = Json.encodeToString(line)
                outputStream?.write((lineJson + "\n").toByteArray())
                outputStream?.flush()
                Log.d("BluetoothViewModel", "Movimiento enviado: $lineJson")
            } catch (e: IOException) {
                Log.e("BluetoothViewModel", "Error al enviar movimiento", e)
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
            Log.e("BluetoothViewModel", "Error al desconectar", e)
        }
        _uiState.update { BluetoothUiState() }
        BluetoothGameManager.updateStatus(ConnectionStatus.Idle) // Notificar al GameManager
        Log.d("BluetoothViewModel", "Desconectado.")
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