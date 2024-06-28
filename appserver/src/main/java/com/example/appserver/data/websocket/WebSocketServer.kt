package com.example.appserver.data.websocket

import android.util.Log
import com.example.appserver.data.EventLogger
import com.example.appserver.domain.EventType
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.util.UUID

interface WebSocketServer {
    val eventsFlow: SharedFlow<ServerWebSocketEvent>
    val isConnected: StateFlow<Boolean>
    val connectedClients: StateFlow<Set<String>>
    fun start(port: String)
    fun stop()
    fun send(clientId: String, message: String)
}

sealed class ServerWebSocketEvent {
    data class ServerStarted(val message: String) : ServerWebSocketEvent()
    data class ClientConnected(val clientId: String) : ServerWebSocketEvent()
    data class ClientDisconnected(val clientId: String, val reason: String?) :
        ServerWebSocketEvent()

    data class MessageReceived(val clientId: String, val message: String) : ServerWebSocketEvent()
    data class ServerStopped(val message: String) : ServerWebSocketEvent()
    data class WebSocketError(val clientId: String, val error: Throwable) : ServerWebSocketEvent()
    data class ServerError(val error: Throwable) : ServerWebSocketEvent()
}

class KtorWebSocketServer : WebSocketServer, KoinComponent {

    private val eventLogger: EventLogger by inject()

    private var _eventsFlow = MutableSharedFlow<ServerWebSocketEvent>(replay = 1)
    override val eventsFlow = _eventsFlow.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedClients = MutableStateFlow<Set<String>>(emptySet())
    override val connectedClients = _connectedClients.asStateFlow()

    private val coroutineScope = CoroutineScope(SupervisorJob())

    private var server: ApplicationEngine? = null
    private val webSocketSessions = mutableMapOf<String, DefaultWebSocketServerSession>()

    override fun start(port: String) {
        coroutineScope.launch(Dispatchers.IO) {
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
                    _isConnected.update { true }
                    launch(Dispatchers.IO) {
                        val msg = "Server started"
                        Log.d("ServerWebSocket", msg)
                        _eventsFlow.emit(ServerWebSocketEvent.ServerStarted(msg))
                    }
                    routing {
                        webSocket("/echo") {
                            val clientId = UUID.randomUUID().toString()
                            webSocketSessions[clientId] = this
                            _connectedClients.update { it + clientId }
                            launch {
                                while (isActive) {
                                    delay(999L)
                                    send(Frame.Text("server message"))
                                }
                            }

                            _eventsFlow.emit(ServerWebSocketEvent.ClientConnected(clientId = clientId))
                            try {
                                incoming.consumeEach { frame ->
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        // Log.d("ServerWebSocket", "Received from $clientId: $text")
                                        if (text != "client message") _eventsFlow.emit(
                                                ServerWebSocketEvent.MessageReceived(
                                                    clientId, text
                                                )
                                            )
                                    } else Log.d(
                                        "ServerWebSocket", "Received from $clientId: non-text frame"
                                    )
                                }
                            } catch (e: Exception) {
                                val msg = "Error while receiving messages: ${e.message}"
                                Log.e("ServerWebSocket", msg, e)
                                _eventsFlow.emit(
                                    ServerWebSocketEvent.WebSocketError(
                                        clientId,
                                        Exception(msg)
                                    )
                                )
                            } finally {
                                val reason = closeReason.await()?.message
                                webSocketSessions.remove(clientId)
                                _connectedClients.update { it - clientId }
                                _eventsFlow.emit(
                                    ServerWebSocketEvent.ClientDisconnected(clientId, reason)
                                )
                                Log.d(
                                    "ServerWebSocket",
                                    "Client disconnected: $clientId, reason: $reason"
                                )
                            }
                        }
                    }
                }.start(wait = false)
            } catch (e: Exception) {
                val msg = "Exception in ServerWebSocket: ${e.message}"
                Log.e("ServerWebSocket", msg, e)
                _eventsFlow.emit(ServerWebSocketEvent.ServerError(Exception(msg)))
                this@KtorWebSocketServer.stop()
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
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d("ServerWebSocket", "stopping")
                server?.stop(1000L, 3333L)
                server = null
                val msg = "Server stopped"
                Log.d("ServerWebSocket", msg)
                _eventsFlow.emit(ServerWebSocketEvent.ServerStopped(msg))

            } catch (e: Exception) {
                val msg = "Exception in stopping server: ${e.message}"
                Log.e("ServerWebSocket", msg, e)
                _eventsFlow.emit(ServerWebSocketEvent.ServerError(Exception(msg)))
            } finally {
                _isConnected.update { false }
            }
        }
    }

    override fun send(clientId: String, message: String) {
        coroutineScope.launch(Dispatchers.IO) {
            webSocketSessions[clientId]?.send(Frame.Text(message))
            eventLogger.logClientEvent(clientId, EventType.MessageSent, message)
        }
    }
}
