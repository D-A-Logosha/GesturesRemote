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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration

interface WebSocketServer {
    val eventsFlow: SharedFlow<ServerWebSocketEvent>
    fun start(port: String)
    fun stop()
}

sealed class ServerWebSocketEvent {
    data class ServerStarted(val message: String) : ServerWebSocketEvent()
    data class ClientConnected(val clientId: String) : ServerWebSocketEvent()
    data class ClientDisconnected(val clientId: String, val reason: String?) :
        ServerWebSocketEvent()

    data class MessageReceived(val clientId: String, val message: String) : ServerWebSocketEvent()
    data class ServerStopped(val message: String) : ServerWebSocketEvent()
    data class WebSocketError(val error: Throwable) : ServerWebSocketEvent()
    data class ServerError(val error: Throwable) : ServerWebSocketEvent()
}

class KtorWebSocketServer : WebSocketServer {

    override var eventsFlow = MutableSharedFlow<ServerWebSocketEvent>(replay = 0)
        private set

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var server: ApplicationEngine? = null

    override fun start(port: String) {
        coroutineScope.launch {
            try {
                if (!isPortAvailable(port.toInt())) {
                    throw Exception("Port $port is already in use")
                }
                server = embeddedServer(CIO, port = port.toInt()) {
                    Log.d("ServerWebSocket", "try install WebSockets $port")
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(32)
                        timeout = Duration.ofSeconds(64)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    launch {
                        val msg = "Server started"
                        Log.d("ServerWebSocket", msg)
                        eventsFlow.emit(ServerWebSocketEvent.ServerStarted(msg))
                    }
                    routing {
                        webSocket("/echo") {
                            launch {
                                while (isActive) {
                                    delay(999L)
                                    send(Frame.Text("server message"))
                                }
                            }

                            val clientId = call.request.headers["clientId"] ?: "unknown"
                            eventsFlow.emit(ServerWebSocketEvent.ClientConnected(clientId = clientId))
                            try {
                                incoming.consumeEach { frame ->
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        Log.d(
                                            "ServerWebSocket",
                                            "Received from $clientId: $text"
                                        )
                                        eventsFlow.emit(
                                            ServerWebSocketEvent.MessageReceived(
                                                clientId, text
                                            )
                                        )
                                    } else Log.d(
                                        "ServerWebSocket",
                                        "Received from $clientId: non-text frame"
                                    )
                                }
                            } catch (e: Exception) {
                                val msg = "Error while receiving messages: ${e.message}"
                                Log.e("ServerWebSocket", msg, e)
                                eventsFlow.emit(ServerWebSocketEvent.WebSocketError(Exception(msg)))
                            } finally {
                                val reason = closeReason.await()?.message
                                Log.d(
                                    "ServerWebSocket",
                                    "Client disconnected: $clientId, reason: $reason"
                                )
                                eventsFlow.emit(
                                    ServerWebSocketEvent.ClientDisconnected(clientId, reason)
                                )
                            }
                        }
                    }
                }.start(wait = false)
            } catch (e: Exception) {
                val msg = "Exception in ServerWebSocket: ${e.message}"
                Log.e("ServerWebSocket", msg, e)
                eventsFlow.emit(ServerWebSocketEvent.ServerError(Exception(msg)))
                eventsFlow.emit(ServerWebSocketEvent.ServerStopped(msg))
            }
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
        coroutineScope.launch {
            try {
                Log.d("ServerWebSocket", "stopping")
                server?.stop(1000L, 3333L)
                server = null
                val msg = "Server stopped"
                Log.d("ServerWebSocket", msg)
                eventsFlow.emit(ServerWebSocketEvent.ServerStopped(msg))

            } catch (e: Exception) {
                val msg = "Exception in stopping server: ${e.message}"
                Log.e("ServerWebSocket", msg, e)
                eventsFlow.emit(ServerWebSocketEvent.ServerError(Exception(msg)))
            }
        }
    }
}