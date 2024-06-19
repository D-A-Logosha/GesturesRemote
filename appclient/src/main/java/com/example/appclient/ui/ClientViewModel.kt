package com.example.appclient.ui

import android.graphics.Point
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appclient.data.websocket.ClientWebSocketEvent
import com.example.appclient.data.websocket.WebSocketClient
import com.example.appclient.domain.GestureServiceManager
import com.example.common.domain.GestureData
import com.example.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ClientViewModel(
    private val settingsRepository: SettingsRepository,
    private val webSocketClient: WebSocketClient,
    private val gestureServiceManager: GestureServiceManager
) : ViewModel(), ClientViewModelInterface {

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

    override fun onConfigClick() {
    }

    override fun onStartPauseClick() {
        when (clientUiState.clientState) {
            ClientState.Stopped -> {
                if (gestureServiceManager.isServiceEnabled()) {
                    clientUiState = clientUiState.copy(clientState = ClientState.Starting)
                    webSocketClient.connect(
                        ipAddress = clientUiState.ipAddress, port = clientUiState.port
                    )
                    startSwipes()
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

    override fun onSaveSettings(newIpAddress: String, newPort: String) {
        clientUiState = clientUiState.copy(ipAddress = newIpAddress, port = newPort)
        settingsRepository.saveClientSettings(
            clientUiState.ipAddress, clientUiState.port.toInt()
        )
    }

    private fun startSwipes() {
        viewModelScope.launch(Dispatchers.IO) {
            var isSwipeDown = true
            while (isActive) {
                if (!gestureServiceManager.isChromeFocused()) {
                    var timeOut = true
                    while (!gestureServiceManager.isChromeFocused()) {
                        if (timeOut) {
                            gestureServiceManager.openChrome()
                            timeOut = false
                            viewModelScope.launch(Dispatchers.IO) {
                                delay(2222L)
                                timeOut = true
                            }
                        }
                    }
                } else {
                    val swipeArea = gestureServiceManager.getChromeSwipeArea()
                    val swipe = if (isSwipeDown) "down" else "up"
                    Log.d(
                        "ClientViewModel",
                        "ClientViewModel: swipe ${swipe}. Window size ${swipeArea.width()}x${swipeArea.height()}"
                    )
                    val x: Int = swipeArea.centerX()
                    val h: Int = swipeArea.height() / 3

                    val gesture = if (isSwipeDown) GestureData(
                        Point(x, swipeArea.top + h), Point(x, swipeArea.bottom - h), 555L
                    ) else GestureData(
                        Point(x, swipeArea.bottom - h), Point(x, swipeArea.top + h), 555L
                    )
                    gestureServiceManager.performSwipe(gesture)
                    isSwipeDown = !isSwipeDown
                    delay(2222L)
                }
            }
        }
    }
}
