package com.example.appclient.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.settings.SettingsRepository

class ClientViewModel(
    private val settingsRepository: SettingsRepository
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
        clientUiState = clientUiState.copy(isClientRun = !clientUiState.isClientRun)
    }

    fun onSaveSettings(newIpAddress: String, newPort: String) {
        clientUiState = clientUiState.copy(ipAddress = newIpAddress, port = newPort)
        settingsRepository.saveClientSettings(
            clientUiState.ipAddress,
            clientUiState.port.toInt()
        )
    }
}