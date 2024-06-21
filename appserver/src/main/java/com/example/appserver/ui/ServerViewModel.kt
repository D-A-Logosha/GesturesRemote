package com.example.appserver.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appserver.data.ServerWebSocketEvent
import com.example.appserver.data.WebSocketServer
import com.example.appserver.domain.usecase.GenerateGestureDataUseCase
import com.example.common.domain.SerializableSwipeArea
import com.example.common.domain.SwipeArea
import com.example.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class ServerViewModel(
    private val settingsRepository: SettingsRepository,
    private val webSocketServer: WebSocketServer,
) : ViewModel(), KoinComponent {

    private val generateGestureDataUseCase: GenerateGestureDataUseCase by inject(parameters = {
        parametersOf(
            viewModelScope
        )
    })

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
        viewModelScope.launch {
            snackbarMessage.emit(message)
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketServer.eventsFlow.collect { event ->
                when (event) {
                    is ServerWebSocketEvent.ServerStarted -> {
                        serverUiState = serverUiState.copy(serverState = ServerState.Running)
                        val msg = "Server started"
                        Log.d("ServerViewModel", msg)
                        sendSnackbarMessage("Event: $msg")
                    }

                    is ServerWebSocketEvent.ClientConnected -> {
                        val msg = "Client connected: ${event.clientId}"
                        Log.d("ServerViewModel", msg)
                        sendSnackbarMessage("Event: $msg")
                    }

                    is ServerWebSocketEvent.ClientDisconnected -> {
                        val msg = "Client disconnected: ${event.clientId}, reason: ${event.reason}"
                        Log.d("ServerViewModel", msg)
                        sendSnackbarMessage("Event: $msg")
                        generateGestureDataUseCase.stop()
                    }

                    is ServerWebSocketEvent.MessageReceived -> {
                        val msg = "Message received from ${event.clientId}: ${event.message}"
                        Log.d("ServerViewModel", msg)
                        if (event.message.startsWith("{") && event.message.endsWith("}")) {
                            try {
                                val swipeAreaJson =
                                    Json.decodeFromString<SerializableSwipeArea>(event.message)
                                generateGestureDataUseCase.start(SwipeArea(swipeAreaJson()))
                            } catch (e: Exception) {
                                Log.e(
                                    "ServerViewModel",
                                    "Error decoding swipe area: ${e.message}",
                                    e
                                )
                            }
                        }
                    }

                    is ServerWebSocketEvent.ServerStopped -> {
                        serverUiState = serverUiState.copy(serverState = ServerState.Stopped)
                        Log.d("ServerViewModel", event.message)
                        sendSnackbarMessage("Event: ${event.message}")
                        generateGestureDataUseCase.stop()
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