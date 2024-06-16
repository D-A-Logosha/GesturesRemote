package com.example.appclient.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appclient.data.WebSocketClient
import com.example.settings.SettingsRepository
import kotlinx.coroutines.launch

class ClientViewModel(
    private val settingsRepository: SettingsRepository,
    private val webSocketClient: WebSocketClient,
) : ViewModel() {

    var clientUiState by mutableStateOf(getInitialClientUiState())
        private set

    private fun getInitialClientUiState(): ClientUiState {
        val savedIpAddress = settingsRepository.getIpAddress()
        return if (savedIpAddress != null) {
            ClientUiState(
                ipAddress = savedIpAddress,
                port = settingsRepository.getServerPort().toString()
            )
        } else {
            ClientUiState()
        }
    }

    fun onConfigClick() {
    }

    fun onStartPauseClick() {
        viewModelScope.launch {
            if (!clientUiState.isClientRun) {
                webSocketClient.connect(
                    ipAddress = clientUiState.ipAddress,
                    port = clientUiState.port
                )
                clientUiState = clientUiState.copy(isClientRun = true)
            } else {
                webSocketClient.disconnect()
                clientUiState = clientUiState.copy(isClientRun = false)
            }
        }
    }

    fun onSaveSettings(newIpAddress: String, newPort: String) {
        clientUiState = clientUiState.copy(ipAddress = newIpAddress, port = newPort)
        settingsRepository.saveClientSettings(
            clientUiState.ipAddress,
            clientUiState.port.toInt()
        )
    }
}