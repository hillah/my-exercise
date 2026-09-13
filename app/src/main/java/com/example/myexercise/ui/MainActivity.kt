package com.example.myexercise.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.health.connect.client.PermissionController
import com.example.myexercise.data.health.HealthConnectManager
import com.example.myexercise.ui.main.MainScreen
import com.example.myexercise.ui.main.MainViewModel
import com.example.myexercise.ui.theme.MyExerciseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application)
    }

    private val healthConnectManager by lazy { HealthConnectManager(this) }

    // Health Connect permissions request launcher
    private val requestPermissionActivityContract =
        PermissionController.createRequestPermissionResultContract()

    private val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(healthConnectManager.permissions)) {
                viewModel.syncHealthConnect()
                com.example.myexercise.worker.HealthSyncWorker.schedulePeriodicSync(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule periodic background sync if Health Connect is supported
        if (healthConnectManager.isAvailable()) {
            com.example.myexercise.worker.HealthSyncWorker.schedulePeriodicSync(this)
        }

        setContent {
            MyExerciseTheme {
                val uiState by viewModel.uiState.collectAsState()

                MainScreen(
                    uiState = uiState,
                    onLogExercise = { type ->
                        viewModel.logQuickExercise(type)
                    },
                    onSyncHealthConnect = {
                        if (healthConnectManager.isAvailable()) {
                            requestPermissions.launch(healthConnectManager.permissions)
                        } else {
                            viewModel.syncHealthConnect()
                        }
                    },
                    onClearSnackBar = {
                        viewModel.clearSnackBarMessage()
                    }
                )
            }
        }
    }
}
