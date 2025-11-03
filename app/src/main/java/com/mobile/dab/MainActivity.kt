package com.mobile.dab

import HistoryScreen
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mobile.dab.bluetooth.BluetoothGameManager // <-- NUEVO
import com.mobile.dab.bluetooth.BluetoothViewModel
import com.mobile.dab.bluetooth.ConnectionStatus
import com.mobile.dab.ui.composable.bluetooth.BluetoothLobbyScreen
import com.mobile.dab.game.GameViewModel
import com.mobile.dab.ui.BluetoothLobby
import com.mobile.dab.ui.GAME_SCREEN
import com.mobile.dab.ui.GameScreen
import com.mobile.dab.ui.HistoryScreen
import com.mobile.dab.ui.MAIN_MENU
import com.mobile.dab.ui.MainMenu
import com.mobile.dab.ui.composable.TopGameAppBar
import com.mobile.dab.ui.composable.game.GamePlayScreen
import com.mobile.dab.ui.composable.menu.MenuScreen
import com.mobile.dab.ui.theme.DotsAndBoxesTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// Esta es la "Primera Versión" de MainActivity
class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels() // Todavía necesario

    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private var showRationaleDialog by mutableStateOf(false)
    private var showSettingsDialog by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val allGranted = perms.values.all { it }
            if (allGranted) {
                findNavController()?.navigate(BluetoothLobby)
            } else {
                showSettingsDialog = true
            }
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                permissionLauncher.launch(permissionsToRequest)
            } else {
                // El usuario no activó Bluetooth.
            }
        }

    private val discoverableLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_CANCELED) {
                bluetoothViewModel.startServer()
            }
        }

    private var navController: NavHostController? = null
    private fun findNavController(): NavHostController? = navController

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- ELIMINADO: El GameViewModel lo hace internamente ahora ---
        // lifecycleScope.launchWhenStarted {
        //     bluetoothViewModel.incomingMove...
        // }

        setContent {
            DotsAndBoxesTheme {
                navController = rememberNavController()
                val currentBackStackEntry by navController!!.currentBackStackEntryAsState()

                val currentScreen = when {
                    currentBackStackEntry?.destination?.hasRoute<MainMenu>() == true -> MAIN_MENU
                    currentBackStackEntry?.destination?.hasRoute<GameScreen>() == true -> GAME_SCREEN
                    currentBackStackEntry?.destination?.hasRoute<BluetoothLobby>() == true -> "BluetoothLobby"
                    else -> ""
                }

                val gameState by gameViewModel.uiState.collectAsState()
                // --- ELIMINADO: Ya no necesitamos el btState aquí ---
                // val btState by bluetoothViewModel.uiState.collectAsState()

                // --- NUEVO: Escuchar al GameManager para la navegación ---
                val connectionStatus by BluetoothGameManager.connectionStatus.collectAsState()

                LaunchedEffect(connectionStatus) {
                    if (connectionStatus == ConnectionStatus.Connected) {
                        // Preguntar al ViewModel (que sabe) si es servidor
                        val isServer = bluetoothViewModel.amIServer

                        // Decirle al GameViewModel que inicie
                        gameViewModel.startBluetoothGame(isServer)

                        // Navegar
                        navController?.navigate(GameScreen) {
                            popUpTo(MainMenu)
                        }
                        // Resetear el estado para evitar bucles
                        BluetoothGameManager.resetConnectionStatus()
                    }
                }
                // --- FIN DEL NUEVO CÓDIGO ---


                if (showRationaleDialog) {
                    BluetoothRationaleDialog(
                        onConfirm = {
                            showRationaleDialog = false
                            permissionLauncher.launch(permissionsToRequest)
                        },
                        onDismiss = { showRationaleDialog = false }
                    )
                }

                if (showSettingsDialog) {
                    BluetoothSettingsDialog(
                        onConfirm = {
                            showSettingsDialog = false
                            openAppSettings()
                        },
                        onDismiss = { showSettingsDialog = false }
                    )
                }

                Scaffold(
                    topBar = {
                        TopGameAppBar(
                            onClickNavigationIcon = {
                                // --- CAMBIO: Desconectar desde el ViewModel ---
                                bluetoothViewModel.disconnect()
                                navController?.popBackStack()
                            },
                            currentScreen = currentScreen,
                            onClickHistory = { navController?.navigate(HistoryScreen) },
                            vsComputer = gameState.isVsComputer
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        modifier = Modifier
							.fillMaxSize()
							.padding(innerPadding),
                        navController = navController!!,
                        startDestination = MainMenu
                    ) {
                        composable<MainMenu> {
                            MenuScreen(
                                modifier = Modifier
									.fillMaxSize()
									.padding(24.dp),
                                onStartLocalGame = { vsComputer ->
                                    gameViewModel.startNewGame(vsComputer)
                                    navController?.navigate(GameScreen)
                                },
                                onStartBluetoothGame = {
                                    checkBluetoothAndPermissions()
                                }
                            )
                        }
                        composable<GameScreen> {
                            GamePlayScreen(
                                modifier = Modifier
									.fillMaxSize()
									.padding(horizontal = 24.dp),
                                state = gameState,
                                onLineSelected = gameViewModel::makeMove
                            )
                        }
                        composable<HistoryScreen> {
                            val history by gameViewModel.history.collectAsState()
                            HistoryScreen(results = history)
                            gameViewModel.loadHistory()
                        }

                        composable<BluetoothLobby> {
                            // --- CAMBIO: El Lobby AHORA usa el btViewModel ---
                            val btLobbyState by bluetoothViewModel.uiState.collectAsState()
                            BluetoothLobbyScreen(
                                state = btLobbyState,
                                onStartScan = bluetoothViewModel::startScan,
                                onStopScan = bluetoothViewModel::stopScan,
                                onMakeDiscoverable = {
                                    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                                    discoverableLauncher.launch(discoverableIntent)
                                },
                                onConnectToDevice = bluetoothViewModel::connectToDevice
                            )
                        }
                    }
                }
            }
        }
    }

    // (checkBluetoothAndPermissions, openAppSettings, onStop - Sin cambios)
    @SuppressLint("MissingPermission")
    private fun checkBluetoothAndPermissions() {
        val btAdapter = (getSystemService(BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter

        if (btAdapter == null) {
            return
        }

        if (!btAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
                    showRationaleDialog = true
                } else {
                    permissionLauncher.launch(permissionsToRequest)
                }
            } else {
                 if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                     showRationaleDialog = true
                 } else {
                    permissionLauncher.launch(permissionsToRequest)
                 }
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
             bluetoothViewModel.stopScan()
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothViewModel.stopScan()
        }
    }
}

// (Diálogos Rationale y Settings sin cambios)
@Composable
fun BluetoothRationaleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bluetooth_permission_rationale_title)) },
        text = { Text(stringResource(R.string.bluetooth_permission_rationale_message)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun BluetoothSettingsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bluetooth_permission_denied_title)) },
        text = { Text(stringResource(R.string.bluetooth_permission_denied_message)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.go_to_settings))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}