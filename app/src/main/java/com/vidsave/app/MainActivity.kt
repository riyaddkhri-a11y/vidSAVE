package com.vidsave.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vidsave.app.ui.components.UpdateDialog
import com.vidsave.app.ui.screens.ChatScreen
import com.vidsave.app.ui.screens.HistoryScreen
import com.vidsave.app.ui.screens.SettingsScreen
import com.vidsave.app.ui.theme.VidSaveTheme
import com.vidsave.app.ui.viewmodel.AppTheme
import com.vidsave.app.ui.viewmodel.ChatViewModel
import com.vidsave.app.ui.viewmodel.HistoryViewModel
import com.vidsave.app.ui.viewmodel.SettingsViewModel
import com.vidsave.app.ui.viewmodel.UpdateViewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val updateStatus by updateViewModel.updateStatus.collectAsStateWithLifecycle()

            val isDarkTheme = when (settingsState.theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            VidSaveTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VidSaveAppNavigation(
                        chatViewModel = chatViewModel,
                        historyViewModel = historyViewModel,
                        settingsViewModel = settingsViewModel,
                        updateViewModel = updateViewModel
                    )

                    // Global Update Dialog
                    UpdateDialog(
                        updateStatus = updateStatus,
                        currentVersionName = updateViewModel.currentVersionName,
                        onDownloadAndInstall = { metadata ->
                            updateViewModel.downloadAndInstall(metadata)
                        },
                        onDismiss = { updateViewModel.dismissDialog() }
                    )
                }
            }
        }
    }
}

@Composable
fun VidSaveAppNavigation(
    chatViewModel: ChatViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        composable("chat") {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") },
                onCheckUpdate = { updateViewModel.checkForUpdates(forceCheck = true) }
            )
        }

        composable("history") {
            HistoryScreen(
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onCheckUpdate = { updateViewModel.checkForUpdates(forceCheck = true) },
                onClearHistory = { historyViewModel.clearHistory() }
            )
        }
    }
}
