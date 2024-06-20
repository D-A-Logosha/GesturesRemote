package com.example.appclient.domain.usecase

import android.util.Log
import com.example.appclient.data.websocket.WebSocketClient
import com.example.appclient.domain.GestureServiceManager
import com.example.common.domain.SerializableSwipeArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Factory
class SendSwipeAreaUseCase(
    private val viewModelScope: CoroutineScope
) : KoinComponent {
    private val webSocketClient: WebSocketClient by inject()
    private val gestureServiceManager: GestureServiceManager by inject()


    private var job: Job? = null

    fun start() {
        job = viewModelScope.launch(Dispatchers.IO) {
            gestureServiceManager.chromeSwipeArea.collect { swipeArea ->
                try {
                    val swipeAreaJson = Json.encodeToString(SerializableSwipeArea(swipeArea))
                    webSocketClient.send(swipeAreaJson)
                    Log.d("SendSwipeAreaUseCase", "Sent swipe area: $swipeAreaJson")
                } catch (e: Exception) {
                    Log.e("SendSwipeAreaUseCase", "Error sending swipe area: ${e.message}", e)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
