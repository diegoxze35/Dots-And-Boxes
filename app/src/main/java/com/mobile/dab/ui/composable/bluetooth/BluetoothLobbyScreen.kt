package com.mobile.dab.ui.composable.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobile.dab.R
import com.mobile.dab.bluetooth.BluetoothUiState
import com.mobile.dab.bluetooth.ConnectionStatus

@SuppressLint("MissingPermission")
@Composable
fun BluetoothLobbyScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onConnectToDevice: (BluetoothDevice) -> Unit
) {
    // (El Column y los botones/estados no cambian)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.connectionStatus == ConnectionStatus.Idle) {
            Button(
                onClick = onMakeDiscoverable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.make_discoverable))
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStartScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.start_scan))
            }
        }

        when (state.connectionStatus) {
            ConnectionStatus.Waiting -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.status_waiting_for_connection))
            }
            ConnectionStatus.Connecting -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Connecting...")
            }
            ConnectionStatus.Connected -> {
                Text(stringResource(R.string.status_connected))
            }
            ConnectionStatus.Idle -> {}
        }

        Spacer(Modifier.height(16.dp))

        if (state.isScanning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.scanning_for_devices))
            }
        }

        if (state.scannedDevices.isNotEmpty()) {
            Text(
                stringResource(R.string.available_devices),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = state.scannedDevices, key = { it.address }) { device ->
                    DeviceItem(device = device, onClick = {
                        onConnectToDevice(device)
                    })
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        // --- INICIO SOLUCIÓN P1 (Descubrimiento) ---
        Text(
            text = device.name ?: "Unknown Host", // Mostrar "Unknown Host"
            fontWeight = FontWeight.Bold
        )
        // --- FIN SOLUCIÓN P1 ---
        Text(text = device.address, style = MaterialTheme.typography.bodySmall)
    }
}