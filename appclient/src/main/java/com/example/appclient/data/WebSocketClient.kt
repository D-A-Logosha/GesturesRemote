package com.example.appclient.data

import android.util.Log
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface WebSocketClient {
    val eventsFlow: SharedFlow<ClientWebSocketEvent>
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

    override var eventsFlow = MutableSharedFlow<ClientWebSocketEvent>(replay = 0)
        private set


    private var webSocketSession: DefaultClientWebSocketSession? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socketJob: Job? = null

    override fun connect(ipAddress: String, port: String) {
        socketJob = coroutineScope.launch {
            try {
                httpClient.webSocket(
                    host = ipAddress,
                    port = port.toInt(),
                    path = "/echo",
                ) {
                    Log.d("ClientWebSocket", "Connected to server")

                    launch {
                        while (isActive) {
                            delay(3333L)
                            send(Frame.Text("client message"))
                        }
                    }

                    webSocketSession = this
                    eventsFlow.emit(ClientWebSocketEvent.Connected(message = "Connected to server"))

                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                Log.d("ClientWebSocket", "Received: $text")
                                eventsFlow.emit(ClientWebSocketEvent.MessageReceived(message = text))
                            }

                            else -> {
                                Log.d("ClientWebSocket", "Received non-text frame")
                            }
                        }
                    }

                    launch {
                        while (isActive) {
                            val result = withTimeoutOrNull(3333L) {
                                eventsFlow.first { it is ClientWebSocketEvent.MessageReceived }
                            }
                            if (result == null) {
                                val timeoutException = Exception("Server timeout")
                                eventsFlow.emit(ClientWebSocketEvent.Error(timeoutException))
                                disconnect()
                                break
                            }
                        }
                    }

                }

            } catch (e: Exception) {
                Log.e("ClientWebSocket", "Error during WebSocket connection: ${e.message}", e)
                eventsFlow.emit(ClientWebSocketEvent.Error(e))
            }
        }
    }

    override fun disconnect() {
        socketJob = null
        coroutineScope.launch {
            try {
                webSocketSession?.close(
                    CloseReason(
                        CloseReason.Codes.NORMAL, "Client disconnected"
                    )
                )
                webSocketSession = null
            } catch (e: Exception) {
                Log.e("ClientWebSocket", "Error during WebSocket disconnect: ${e.message}", e)
            } finally {
                eventsFlow.emit(ClientWebSocketEvent.Disconnected(message = "Disconnected from server"))
            }
        }
    }

    override fun send(message: String) {
        coroutineScope.launch {
            if (webSocketSession?.isActive == true) {
                webSocketSession?.send(Frame.Text(message))
            }
        }
    }
}