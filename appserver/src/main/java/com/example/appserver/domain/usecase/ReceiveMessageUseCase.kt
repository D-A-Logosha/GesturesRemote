package com.example.appserver.domain.usecase

import android.util.Log
import com.example.appserver.data.websocket.ServerWebSocketEvent
import com.example.appserver.data.websocket.WebSocketServer
import com.example.appserver.domain.interfaces.ChromeSwipeAreaProviders
import com.example.appserver.domain.interfaces.PerformedGesturesProviders
import com.example.common.domain.Message
import com.example.common.domain.PerformedGesture
import com.example.common.domain.SerializableSwipeArea
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Factory
class ReceiveMessageUseCase(
    private val clientId: String,
    private val useCaseScope: CoroutineScope,
) : ChromeSwipeAreaProviders, PerformedGesturesProviders, KoinComponent {
    private val webSocketServer: WebSocketServer by inject()

    private val json = Json { classDiscriminator = "type" }

    private val _isProviderAvailable = MutableStateFlow(false)
    override val isProviderAvailable = _isProviderAvailable.asStateFlow()

    private val _chromeSwipeArea = MutableStateFlow(SwipeArea())
    override val chromeSwipeArea = _chromeSwipeArea.asStateFlow()

    private val _performedGestures = MutableSharedFlow<PerformedGesture>(replay = 0)
    override val performedGestures = _performedGestures.asSharedFlow()

    private var job: Job? = null

    fun start() {
        job = useCaseScope.launch {
            webSocketServer.eventsFlow.collect { event ->
                when (event) {
                    is ServerWebSocketEvent.MessageReceived -> {
                        if (event.clientId != clientId) return@collect
                        if (event.message.startsWith("{") && event.message.endsWith("}")) {
                            try {
                                val message = Json.decodeFromString<Message>(event.message)
                                when (message.type) {
                                    "swipeArea" -> {
                                        val swipeArea =
                                            Json.decodeFromJsonElement<SerializableSwipeArea>(
                                                message.data
                                            )
                                        _chromeSwipeArea.update { swipeArea() }
                                        _isProviderAvailable.update { true }
                                        Log.d(
                                            "ReceiveMessageUseCase",
                                            "Decoding SwipeArea: $swipeArea",
                                        )
                                    }

                                    "performedGesture" -> {
                                        val performedGesture =
                                            Json.decodeFromJsonElement<PerformedGesture>(message.data)
                                        Log.d(
                                            "ReceiveMessageUseCase",
                                            "Decoding PerformedGesture: $performedGesture"
                                        )
                                    }

                                    else -> {
                                        Log.e(
                                            "ReceiveMessageUseCase",
                                            "Unknown message type: ${event.message}",
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "ReceiveMessageUseCase",
                                    "Error decoding message: ${e.message}",
                                    e
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
