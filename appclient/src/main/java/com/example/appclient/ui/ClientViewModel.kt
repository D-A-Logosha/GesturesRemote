package com.example.appclient.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appclient.data.websocket.ClientWebSocketEvent
import com.example.appclient.data.websocket.WebSocketClient
import com.example.appclient.domain.GestureAccessibilityManager
import com.example.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class ClientViewModel(
    private val settingsRepository: SettingsRepository,
    private val webSocketClient: WebSocketClient,
    private val accessibilityManager: GestureAccessibilityManager
) : ViewModel() {

    var clientUiState by mutableStateOf(getInitialClientUiState())
        private set

    var snackbarMessage = MutableSharedFlow<String>(replay = 0)
        private set


    init {
        observeWebSocketEvents()
    }

    private fun getInitialClientUiState(): ClientUiState {
        val savedIpAddress = settingsRepository.getIpAddress()
        return if (savedIpAddress != null) {
            ClientUiState(
                ipAddress = savedIpAddress, port = settingsRepository.getServerPort().toString()
            )
        } else {
            ClientUiState()
        }
    }

    private fun sendSnackbarMessage(message: String) {
        viewModelScope.launch {
            snackbarMessage.emit(message)
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketClient.eventsFlow.collect { event ->
                when (event) {
                    is ClientWebSocketEvent.Connected -> {
                        clientUiState = clientUiState.copy(clientState = ClientState.Started)
                        Log.d("ClientViewModel", "Event: Connected to server")
                        sendSnackbarMessage("Event: Connected to server")
                    }

                    is ClientWebSocketEvent.Disconnected -> {
                        clientUiState = clientUiState.copy(clientState = ClientState.Stopped)
                        Log.d("ClientViewModel", "Event: Disconnected from server")
                        sendSnackbarMessage("Event: Disconnected from server")
                    }

                    is ClientWebSocketEvent.Error -> {
                        clientUiState = clientUiState.copy(clientState = ClientState.Stopped)
                        Log.e("ClientViewModel", "Event: WebSocket error: ${event.error}")
                        sendSnackbarMessage("Event: WebSocket error: ${event.error}")
                    }

                    is ClientWebSocketEvent.MessageReceived -> {
                        Log.d("ClientViewModel", "Event: received: ${event.message}")
                    }
                }
            }
        }
    }

    fun onConfigClick() {
    }

    fun onStartPauseClick() {
        when (clientUiState.clientState) {
            ClientState.Stopped -> {
                if (accessibilityManager.isServiceEnabled()) {
                    clientUiState = clientUiState.copy(clientState = ClientState.Starting)
                    webSocketClient.connect(
                        ipAddress = clientUiState.ipAddress, port = clientUiState.port
                    )
                } else {
                    sendSnackbarMessage("Accessibility Service is not enabled")
                }
            }

            ClientState.Started -> {
                clientUiState = clientUiState.copy(clientState = ClientState.Stopping)
                webSocketClient.disconnect()
            }

            else -> {}
        }
    }

    fun onSaveSettings(newIpAddress: String, newPort: String) {
        clientUiState = clientUiState.copy(ipAddress = newIpAddress, port = newPort)
        settingsRepository.saveClientSettings(
            clientUiState.ipAddress, clientUiState.port.toInt()
        )
    }
}
