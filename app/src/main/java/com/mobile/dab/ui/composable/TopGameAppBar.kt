package com.mobile.dab.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mobile.dab.R
import com.mobile.dab.ui.GAME_SCREEN
import com.mobile.dab.ui.MAIN_MENU

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopGameAppBar(
    modifier: Modifier = Modifier,
    onClickNavigationIcon: () -> Unit,
    currentScreen: String,
    onClickHistory: () -> Unit,
    vsComputer: Boolean
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                if (currentScreen == GAME_SCREEN) {
                    Text(
                        text = if (vsComputer) "Vs Computer" else "Local",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        navigationIcon = {
            if (currentScreen != MAIN_MENU) {
                IconButton(onClick = onClickNavigationIcon) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        },
        actions = {
            if (currentScreen == MAIN_MENU) {
                Button(onClick = onClickHistory) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null
                    )
                    Text(text = "History")
                }
            }
        }
    )
}
