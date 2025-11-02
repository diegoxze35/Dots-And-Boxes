package com.mobile.dab

import HistoryScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mobile.dab.game.GameViewModel
import com.mobile.dab.ui.GAME_SCREEN
import com.mobile.dab.ui.GameScreen
import com.mobile.dab.ui.HistoryScreen
import com.mobile.dab.ui.MAIN_MENU
import com.mobile.dab.ui.MainMenu
import com.mobile.dab.ui.composable.TopGameAppBar
import com.mobile.dab.ui.composable.game.GamePlayScreen
import com.mobile.dab.ui.composable.menu.MenuScreen
import com.mobile.dab.ui.theme.DotsAndBoxesTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DotsAndBoxesTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentScreen = when {
                    currentBackStackEntry?.destination?.hasRoute<MainMenu>() == true -> MAIN_MENU
                    currentBackStackEntry?.destination?.hasRoute<GameScreen>() == true -> GAME_SCREEN
                    else -> ""
                }
                val gameState by viewModel.uiState.collectAsState()
                Scaffold(
                    topBar = {
                        TopGameAppBar(
                            onClickNavigationIcon = { navController.popBackStack() },
                            currentScreen = currentScreen,
                            onClickHistory = { navController.navigate(HistoryScreen) },
                            vsComputer = gameState.isVsComputer
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        modifier = Modifier
							.fillMaxSize()
							.padding(innerPadding),
                        navController = navController,
                        startDestination = MainMenu
                    ) {
                        composable<MainMenu> {
                            MenuScreen(
                                modifier = Modifier
									.fillMaxSize()
									.padding(24.dp),
                                onStart = { vsComputer ->
                                    viewModel.startNewGame(vsComputer)
                                    navController.navigate(GameScreen)
                                }
                            )
                        }
                        composable<GameScreen> {
                            GamePlayScreen(
                                modifier = Modifier
									.fillMaxSize()
									.padding(horizontal = 24.dp),
                                state = gameState,
                                onLineSelected = viewModel::makeMove
                            )
                        }
                        composable<HistoryScreen> {
                            val history by viewModel.history.collectAsState()
                            HistoryScreen(results = history)
                            viewModel.loadHistory()
                        }
                    }
                }
            }
        }
    }
}
