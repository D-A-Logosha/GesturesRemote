package com.example.appclient.domain.usecase

import android.util.Log
import com.example.appclient.data.websocket.ClientWebSocketEvent
import com.example.appclient.data.websocket.WebSocketClient
import com.example.common.domain.GestureData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Factory
class ReceiveGestureUseCase(
    private val viewModelScope: CoroutineScope
) : KoinComponent {
    private val webSocketClient: WebSocketClient by inject()

    private var job: Job? = null

    private val _receivedGestureFlow = MutableSharedFlow<GestureData>(replay = 0)
    val receivedGestureFlow = _receivedGestureFlow.asSharedFlow()

    fun start() {
        job = viewModelScope.launch {
            webSocketClient.eventsFlow.collect { event ->
                when (event) {
                    is ClientWebSocketEvent.MessageReceived -> {
                        if (event.message.startsWith("{") && event.message.endsWith("}")) {
                            try {
                                val gestureData = GestureData.fromJson(event.message)
                                _receivedGestureFlow.emit(gestureData)
                                Log.d("ReceiveGestureUseCase", "Received gesture: ${gestureData.toString()}")
                            } catch (e: Exception) {
                                Log.e(
                                    "ReceiveGestureUseCase",
                                    "Error decoding gesture data: ${e.message}",
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
