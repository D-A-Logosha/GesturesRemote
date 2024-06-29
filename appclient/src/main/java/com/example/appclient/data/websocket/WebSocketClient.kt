package com.example.appclient.data.websocket

import android.util.Log
import com.example.appclient.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface WebSocketClient {
    val eventsFlow: SharedFlow<ClientWebSocketEvent>
    val isConnected: StateFlow<Boolean>
    fun connect(ipAddress: String, port: String)
    fun disconnect()
    fun send(message: String)
}

sealed class ClientWebSocketEvent {
    data class Connected(val message: String) : ClientWebSocketEvent()
    data class Disconnected(val message: String) : ClientWebSocketEvent()
    data class MessageReceived(val message: String) : ClientWebSocketEvent()
    data class Error(val error: Throwable) : ClientWebSocketEvent()
}

class KtorWebSocketClient : WebSocketClient, KoinComponent {

    private val httpClient: HttpClient by inject()

    override val eventsFlow = MutableSharedFlow<ClientWebSocketEvent>(replay = 0)

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var webSocketSession: DefaultClientWebSocketSession? = null

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private var socketJob: Job? = null

    override fun connect(ipAddress: String, port: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                httpClient.webSocket(
                    host = ipAddress,
                    port = port.toInt(),
                    path = "/echo",
                ) {
                    if (BuildConfig.LOG_LVL>7) Log.d("ClientWebSocket", "Connected to server")

                    launch {
                        while (isActive) {
                            delay(3333L)
                            send(Frame.Text("client message"))
                        }
                    }

                    webSocketSession = this
                    _isConnected.update { true }
                    eventsFlow.emit(ClientWebSocketEvent.Connected(message = "Connected to server"))

                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                if (BuildConfig.LOG_LVL>8) Log.d("ClientWebSocket", "Received: $text")
                                eventsFlow.emit(ClientWebSocketEvent.MessageReceived(message = text))
                            }

                            else -> {
                                if (BuildConfig.LOG_LVL>7) Log.d("ClientWebSocket", "Received non-text frame")
                            }
                        }
                    }

                    socketJob = coroutineScope.launch(Dispatchers.IO) {
                        while (isActive) {
                            val result = withTimeoutOrNull(3333L) {
                                eventsFlow.first { it is ClientWebSocketEvent.MessageReceived }
                            }
                            if (result == null) {
                                if (socketJob?.isActive == true) {
                                    val timeoutException = Exception("Server timeout")
                                    eventsFlow.emit(ClientWebSocketEvent.Error(timeoutException))
                                    disconnect()
                                }
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.LOG_LVL>3) Log.e("ClientWebSocket", "Error during WebSocket connection: ${e.message}", e)
                eventsFlow.emit(ClientWebSocketEvent.Error(e))
            }
        }
    }

    override fun disconnect() {
        coroutineScope.launch(Dispatchers.IO) {
            socketJob?.cancel()
            socketJob = null
            try {
                webSocketSession?.close(
                    CloseReason(
                        CloseReason.Codes.NORMAL, "Client disconnected"
                    )
                )
                webSocketSession = null
            } catch (e: Exception) {
                if (BuildConfig.LOG_LVL>3) Log.e("ClientWebSocket", "Error during WebSocket disconnect: ${e.message}", e)
            } finally {
                _isConnected.update { false }
                eventsFlow.emit(ClientWebSocketEvent.Disconnected(message = "Disconnected from server"))
            }
        }
    }

    override fun send(message: String) {
        coroutineScope.launch(Dispatchers.IO) {
            if (webSocketSession?.isActive == true) {
                webSocketSession?.send(Frame.Text(message))
            }
        }
    }
}
