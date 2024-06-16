package com.example.appserver.data

import android.util.Log
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration

interface WebSocketServer {
    val eventsFlow: SharedFlow<ServerWebSocketEvent>
    fun start(port: String): Boolean
    fun stop()
}

sealed class ServerWebSocketEvent {
    data class ClientConnected(val clientId: String) : ServerWebSocketEvent()
    data class ClientDisconnected(val clientId: String, val reason: String?) : ServerWebSocketEvent()
    data class MessageReceived(val clientId: String, val message: String) : ServerWebSocketEvent()
}

class KtorWebSocketServer : WebSocketServer {

    private val _eventsFlow = MutableSharedFlow<ServerWebSocketEvent>(replay = 0)
    override val eventsFlow: SharedFlow<ServerWebSocketEvent> = _eventsFlow.asSharedFlow()

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var server: ApplicationEngine? = null

    override fun start(port: String): Boolean {
        return try {

            if (!isPortAvailable(port.toInt())) {
                Log.e("ServerWebSocket", "Port $port is already in use")
                // Можно выбросить исключение или вернуть false,
                // чтобы сообщить об ошибке в ViewModel
                return false
            }

            coroutineScope.launch {
                server = embeddedServer(CIO, port = port.toInt()) {
                    Log.d("ServerWebSocket", "try install WebSockets $port")
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(32)
                        timeout = Duration.ofSeconds(64)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    Log.d("ServerWebSocket", "try to routing")

                    routing {
                        webSocket("/echo") {

                            Log.d("ServerWebSocket", "created")

                            launch {
                                while (true) {
                                    delay(3333L)
                                    send(Frame.Text("server message"))
                                }
                            }

                            val clientId = call.request.headers["clientId"] ?: "unknown"
                            _eventsFlow.tryEmit(ServerWebSocketEvent.ClientConnected(clientId = clientId))
                            try {
                                incoming.consumeEach { frame ->
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        Log.d("ServerWebSocket", "Received: $text")
                                        _eventsFlow.tryEmit(
                                            ServerWebSocketEvent.MessageReceived(
                                                clientId,
                                                text
                                            )
                                        )
                                    } else Log.d("ServerWebSocket", "Received non-text frame")
                                }
                            } catch (e: Exception) {
                                Log.d(
                                    "ServerWebSocket",
                                    "Error while receiving messages: ${e.localizedMessage}"
                                )
                            } finally {
                                val reason = closeReason.await()?.message
                                _eventsFlow.tryEmit(
                                    ServerWebSocketEvent.ClientDisconnected(
                                        clientId,
                                        reason
                                    )
                                )
                            }
                        }
                    }
                }.start(wait = false)
            }
            true
        } catch (e: Exception) {
            Log.e("ServerWebSocket", "Exception in startServer: ${e.localizedMessage}", e)
            false
        }
    }

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            java.net.ServerSocket(port).use {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun stop() {
        try {
            coroutineScope.launch(Dispatchers.IO) {
                Log.d("ServerWebSocket", "stopping")
                server?.stop(1000L, 3333L)
                server = null
            }
        } catch (e: Exception) {
            Log.e("ServerWebSocket", "Exception in stopServer: ${e.localizedMessage}", e)
        }
    }
}