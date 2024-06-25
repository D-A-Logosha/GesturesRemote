package com.example.appserver.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appserver.data.websocket.ServerWebSocketEvent
import com.example.appserver.data.websocket.WebSocketServer
import com.example.appserver.domain.usecase.UseCaseManager
import com.example.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class ServerViewModel(
    private val settingsRepository: SettingsRepository,
    private val webSocketServer: WebSocketServer,
) : ViewModel(), KoinComponent {

    private var useCaseManagers: MutableMap<String, UseCaseManager> = mutableMapOf()

    var serverUiState by mutableStateOf(getInitialServerUiState())
        private set

    var snackbarMessage = MutableSharedFlow<String>(replay = 0)
        private set

    init {
        observeWebSocketEvents()
    }

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

    private fun sendSnackbarMessage(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            snackbarMessage.emit(message)
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            webSocketServer.eventsFlow.collect { event ->
                when (event) {
                    is ServerWebSocketEvent.ServerStarted -> {
                        serverUiState = serverUiState.copy(serverState = ServerState.Running)
                        val msg = "Server started"
                        Log.d("ServerViewModel", msg)
                        sendSnackbarMessage("Event: $msg")
                    }

                    is ServerWebSocketEvent.ClientConnected -> {
                        useCaseManagers[event.clientId] =
                            UseCaseManager(event.clientId, viewModelScope)
                        useCaseManagers[event.clientId]?.start()
                        val msg = "Client connected: ${event.clientId}"
                        Log.d("ServerViewModel", msg)
                        sendSnackbarMessage("Event: $msg")
                    }

                    is ServerWebSocketEvent.ClientDisconnected -> {
                        useCaseManagers[event.clientId]?.stop()
                        useCaseManagers.remove(event.clientId)
                        val msg = "Client disconnected: ${event.clientId}, reason: ${event.reason}"
                        Log.d("ServerViewModel", msg)
                        sendSnackbarMessage("Event: $msg")
                    }

                    is ServerWebSocketEvent.MessageReceived -> {
                        val msg = "Message received from ${event.clientId}: ${event.message}"
                        Log.d("ServerViewModel", msg)
                    }

                    is ServerWebSocketEvent.ServerStopped -> {
                        serverUiState = serverUiState.copy(serverState = ServerState.Stopped)
                        useCaseManagers.forEach { (_, useCaseManager) ->
                            useCaseManager.stop()
                        }
                        useCaseManagers.clear()
                        Log.d("ServerViewModel", event.message)
                        sendSnackbarMessage("Event: ${event.message}")
                    }

                    is ServerWebSocketEvent.WebSocketError -> {
                        Log.e("ServerViewModel", "${event.error.message}")
                        sendSnackbarMessage("Receive error: ${event.error.message}")
                    }

                    is ServerWebSocketEvent.ServerError -> {
                        serverUiState = serverUiState.copy(serverState = ServerState.Stopped)
                        Log.e("ServerViewModel", "${event.error.message}")
                        sendSnackbarMessage("Error: ${event.error.message}")
                    }
                }
            }
        }
    }

    fun onConfigClick() {
    }

    fun onStartClick() {
        if (serverUiState.serverState == ServerState.Stopped) {
            webSocketServer.start(serverUiState.port)
            serverUiState = serverUiState.copy(serverState = ServerState.Starting)
        }
    }

    fun onStopClick() {
        if (serverUiState.serverState == ServerState.Running) {
            webSocketServer.stop()
            serverUiState = serverUiState.copy(serverState = ServerState.Stopping)
        }
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
