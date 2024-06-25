package com.example.appclient.domain.usecase

import android.util.Log
import com.example.appclient.data.websocket.WebSocketClient
import com.example.appclient.domain.interfaces.ChromeSwipeAreaProvider
import com.example.appclient.domain.interfaces.GestureServiceManager
import com.example.appclient.domain.interfaces.PerformedGesturesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class UseCaseManager(
    private val viewModelScope: CoroutineScope
) : KoinComponent {

    private val webSocketClient: WebSocketClient by inject()
    private val gestureServiceManager: GestureServiceManager by inject()
    private val chromeSwipeAreaProvider: ChromeSwipeAreaProvider by inject()
    private val performedGesturesProvider: PerformedGesturesProvider by inject()
    private val sendMessageUseCase: SendMessageUseCase by inject(parameters = {
        parametersOf(useCaseScope)
    })

    private val useCaseScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())


    private var job: Job? = null

    fun start() {
        job = viewModelScope.launch(Dispatchers.IO) {
            combine(
                performedGesturesProvider.isServiceEnabled,
                webSocketClient.isConnected,
                gestureServiceManager.isChromeVisibleToUser,
            ) { isServiceEnabled, isWebSocketConnected, isChromeVisible ->
                isServiceEnabled && isWebSocketConnected && isChromeVisible
            }.collect { shouldSendMessages ->
                if (shouldSendMessages) {
                    sendMessageUseCase.startSwipeArea(
                        chromeSwipeAreaProvider.chromeSwipeArea
                    )
                    Log.d("UseCaseManager", "Send swipe area started")
                    sendMessageUseCase.startPerformedGesture(
                        performedGesturesProvider.performedGestures
                    )
                    Log.d("UseCaseManager", "Send performed gestures started")
                } else {
                    sendMessageUseCase.stop(
                        chromeSwipeAreaProvider.chromeSwipeArea
                    )
                    Log.d("UseCaseManager", "Send swipe area stopped")
                    sendMessageUseCase.stop(performedGesturesProvider.performedGestures)
                    Log.d("UseCaseManager", "Send performed gestures stopped")
                }
            }
        }
    }

    fun stop() {
        sendMessageUseCase.stop()
        job?.cancel()
        job = null
        sendMessageUseCase.stop()
    }
}
