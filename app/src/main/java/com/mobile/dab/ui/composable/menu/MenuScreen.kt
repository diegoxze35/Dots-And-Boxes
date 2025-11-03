package com.mobile.dab.ui.composable.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobile.dab.R

@Composable
fun MenuScreen(
    modifier: Modifier = Modifier,
    onStartLocalGame: (vsComputer: Boolean) -> Unit,
    onStartBluetoothGame: () -> Unit // Nuevo callback
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Dots & Boxes", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onStartLocalGame(false) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.two_players)) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onStartLocalGame(true) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.vs_computer)) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onStartBluetoothGame,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.bluetooth_multiplayer)) }
    }
}
