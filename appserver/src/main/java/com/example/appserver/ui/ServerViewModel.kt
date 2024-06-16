package com.example.appserver.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.settings.SettingsRepository

class ServerViewModel(
    private val settingsRepository: SettingsRepository
): ViewModel() {

    var serverUiState by mutableStateOf(getInitialServerUiState())
        private set

    private fun getInitialServerUiState(): ServerUiState {
        val savedPort = settingsRepository.getServerPort()
        return if (savedPort != 0) {
            ServerUiState(
                port = savedPort.toString()
            )
        } else {
            ServerUiState()
        }
    }

    fun onConfigClick() {}

    fun onStartClick() {
        serverUiState = serverUiState.copy(isServerRun = true)
    }

    fun onStopClick() {
        serverUiState = serverUiState.copy(isServerRun = false)
    }

    fun onLogsClick() {}

    fun onSaveSettings(newPort: String) {
        serverUiState = serverUiState.copy(port = newPort)
        settingsRepository.saveServerSettings(
            serverUiState.port.toInt()
        )
    }
}