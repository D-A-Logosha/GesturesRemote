package com.example.appserver.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appserver.data.EventLogger
import com.example.appserver.data.websocket.ServerWebSocketEvent
import com.example.appserver.data.websocket.WebSocketServer
import com.example.appserver.domain.EventType
import com.example.appserver.domain.usecase.UseCaseManager
import com.example.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ServerViewModel(
    private val settingsRepository: SettingsRepository,
    private val webSocketServer: WebSocketServer,
) : ViewModel(), KoinComponent {

    private var useCaseManagers: MutableMap<String, UseCaseManager> = mutableMapOf()

    private val eventLogger: EventLogger by inject()

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
                        eventLogger.logServerEvent(EventType.ServerStarted)
                        Log.d("ServerViewModel", event.message)
                        sendSnackbarMessage("Event: $event.message")
                    }

                    is ServerWebSocketEvent.ClientConnected -> {
                        useCaseManagers[event.clientId] =
                            UseCaseManager(event.clientId, viewModelScope)
                        useCaseManagers[event.clientId]?.start()
                        eventLogger.logClientEvent(event.clientId, EventType.ClientConnected)
                        val msg = "Client connected: ${event.clientId}"
                        Log.d("ServerViewModel", msg)
                        sendSnackbarMessage("Event: $msg")
                    }

                    is ServerWebSocketEvent.ClientDisconnected -> {
                        useCaseManagers[event.clientId]?.stop()
                        useCaseManagers.remove(event.clientId)
                        eventLogger.logClientEvent(
                            event.clientId, EventType.ClientDisconnected, event.reason
                        )
                        val msg = "Client disconnected: ${event.clientId}, reason: ${event.reason}"
                        Log.d("ServerViewModel", msg)
                        sendSnackbarMessage("Event: $msg")
                    }

                    is ServerWebSocketEvent.MessageReceived -> {
                        eventLogger.logClientEvent(
                            event.clientId, EventType.MessageReceived, event.message
                        )
                        val msg = "Message received from ${event.clientId}: ${event.message}"
                        Log.d("ServerViewModel", msg)
                    }

                    is ServerWebSocketEvent.ServerStopped -> {
                        serverUiState = serverUiState.copy(serverState = ServerState.Stopped)
                        useCaseManagers.forEach { (_, useCaseManager) ->
                            useCaseManager.stop()
                        }
                        useCaseManagers.clear()
                        eventLogger.logServerEvent(EventType.ServerStopped)
                        Log.d("ServerViewModel", event.message)
                        sendSnackbarMessage("Event: ${event.message}")
                    }

                    is ServerWebSocketEvent.WebSocketError -> {
                        eventLogger.logClientEvent(
                            event.clientId, EventType.Error, event.error.message
                        )
                        Log.e("ServerViewModel", "${event.clientId}: ${event.error.message}")
                        sendSnackbarMessage("Receive error:${event.clientId}: ${event.error.message}")
                    }

                    is ServerWebSocketEvent.ServerError -> {
                        serverUiState = serverUiState.copy(serverState = ServerState.Stopped)
                        eventLogger.logServerEvent(EventType.Error, event.error.message)
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
            serverUiState = serverUiState.copy(serverState = ServerState.Starting)
            webSocketServer.start(serverUiState.port)
            eventLogger.logServerEvent(EventType.ServerStarting)
        }
    }

    fun onStopClick() {
        if (serverUiState.serverState == ServerState.Running) {
            serverUiState = serverUiState.copy(serverState = ServerState.Stopping)
            webSocketServer.stop()
            eventLogger.logServerEvent(EventType.ServerStopping)
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
