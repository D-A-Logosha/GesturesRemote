package com.example.appclient.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appclient.data.websocket.ClientWebSocketEvent
import com.example.appclient.data.websocket.WebSocketClient
import com.example.appclient.domain.interfaces.GestureServiceManager
import com.example.appclient.domain.usecase.ExecuteGestureUseCase
import com.example.appclient.domain.usecase.ReceiveGestureUseCase
import com.example.appclient.domain.usecase.SendSwipeAreaUseCase
import com.example.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Scope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@Scope
class ClientViewModel(
    private val settingsRepository: SettingsRepository,
    private val webSocketClient: WebSocketClient,
    private val gestureServiceManager: GestureServiceManager,
) : ViewModel(), ClientViewModelInterface, KoinComponent {

    private val sendSwipeAreaUseCase: SendSwipeAreaUseCase by inject(parameters = {
        parametersOf(viewModelScope)
    })
    private val receiveGestureUseCase: ReceiveGestureUseCase by inject(parameters = {
        parametersOf(viewModelScope)
    })
    private val executeGestureUseCase: ExecuteGestureUseCase by inject(parameters = {
        parametersOf(viewModelScope)
    })

    override var clientUiState by mutableStateOf(getInitialClientUiState())
        private set

    override var snackbarMessage = MutableSharedFlow<String>(replay = 0)
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
        viewModelScope.launch(Dispatchers.IO) {
            snackbarMessage.emit(message)
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            webSocketClient.eventsFlow.collect { event ->
                when (event) {
                    is ClientWebSocketEvent.Connected -> {
                        clientUiState = clientUiState.copy(clientState = ClientState.Started)
                        Log.d("ClientViewModel", "Event: Connected to server")
                        sendSnackbarMessage("Event: Connected to server")
                        sendSwipeAreaUseCase.start()
                        receiveGestureUseCase.start()
                        executeGestureUseCase.start(receiveGestureUseCase.receivedGestureFlow)
                    }

                    is ClientWebSocketEvent.Disconnected -> {
                        clientUiState = clientUiState.copy(clientState = ClientState.Stopped)
                        Log.d("ClientViewModel", "Event: Disconnected from server")
                        sendSnackbarMessage("Event: Disconnected from server")
                        sendSwipeAreaUseCase.stop()
                        receiveGestureUseCase.stop()
                        executeGestureUseCase.stop()
                    }

                    is ClientWebSocketEvent.Error -> {
                        clientUiState = clientUiState.copy(clientState = ClientState.Stopped)
                        Log.e("ClientViewModel", "Event: WebSocket error: ${event.error}")
                        sendSnackbarMessage("Event: WebSocket error: ${event.error}")
                        sendSwipeAreaUseCase.stop()
                        receiveGestureUseCase.stop()
                        executeGestureUseCase.stop()
                    }

                    is ClientWebSocketEvent.MessageReceived -> {
                        // Log.d("ClientViewModel", "Event: received: ${event.message}")
                    }
                }
            }
        }
    }

    override fun onConfigClick() {
    }

    override fun onStartPauseClick() {
        viewModelScope.launch(Dispatchers.IO) {
            when (clientUiState.clientState) {
                ClientState.Stopped -> {
                    if (gestureServiceManager.isServiceEnabled()) {
                        if (gestureServiceManager.openChrome()) {
                            clientUiState = clientUiState.copy(clientState = ClientState.Starting)
                            webSocketClient.connect(
                                ipAddress = clientUiState.ipAddress, port = clientUiState.port
                            )
                        } else {
                            sendSnackbarMessage("Chrome not found")
                        }
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
    }

    override fun onSaveSettings(newIpAddress: String, newPort: String) {
        clientUiState = clientUiState.copy(ipAddress = newIpAddress, port = newPort)
        settingsRepository.saveClientSettings(
            clientUiState.ipAddress, clientUiState.port.toInt()
        )
    }
}
