package com.example.appserver.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appserver.data.WebSocketServer
import com.example.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ServerViewModel(
    private val settingsRepository: SettingsRepository,
    private val webSocketServer: WebSocketServer,
) : ViewModel() {

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

    fun onConfigClick() {
    }

    fun onStartClick() {
            if (!serverUiState.isServerRun) {
                if (webSocketServer.start(serverUiState.port)) {
                    serverUiState = serverUiState.copy(isServerRun = true)
                } else {
                    Log.d("ServerWebSocket", "port busy")
                }
            }
    }

    fun onStopClick() {
        if (serverUiState.isServerRun) {
            webSocketServer.stop()
        }
        serverUiState = serverUiState.copy(isServerRun = false)

    }

    fun onLogsClick() {
    }

    fun onSaveSettings(newPort: String) {
        serverUiState = serverUiState.copy(port = newPort)
        settingsRepository.saveServerSettings(
            serverUiState.port.toInt()
        )
    }
}