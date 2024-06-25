package com.example.appserver.domain.usecase

import android.util.Log
import com.example.appserver.data.websocket.WebSocketServer
import com.example.appserver.domain.interfaces.ChromeSwipeAreaProviders
import com.example.appserver.domain.interfaces.PerformedGesturesProviders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UseCaseManager(
    private val clientId: String,
    private val viewModelScope: CoroutineScope,
) : KoinComponent {

    private val webSocketServer: WebSocketServer by inject()

    private val useCaseScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())

    private var job: Job? = null

    private val receiveMessageUseCase = ReceiveMessageUseCase(clientId, useCaseScope)
    private val generateGestureDataUseCase = GenerateGestureDataUseCase(useCaseScope)
    private val sendMessageUseCase = SendMessageUseCase(clientId, useCaseScope)
    private val chromeSwipeAreaProvider = receiveMessageUseCase as ChromeSwipeAreaProviders
    private val performedGesturesProvider = receiveMessageUseCase as PerformedGesturesProviders

    fun start() {
        job = useCaseScope.launch(Dispatchers.IO) {
            useCaseScope.launch(Dispatchers.IO) {
                webSocketServer.connectedClients.collect { connectedClients ->
                    if (connectedClients.contains(clientId)) {
                        receiveMessageUseCase.start()
                        Log.d("UseCaseManager", "Receive messages started")
                    } else {
                        receiveMessageUseCase.stop()
                        Log.d("UseCaseManager", "Receive messages stopped")
                    }
                }

            }
            useCaseScope.launch(Dispatchers.IO) {
                useCaseScope.launch(Dispatchers.IO) {
                    chromeSwipeAreaProvider.isProviderAvailable.collect { isAvailable ->
                        if (isAvailable) {
                            sendMessageUseCase.start(generateGestureDataUseCase.gestureFlow)
                            chromeSwipeAreaProvider.chromeSwipeArea.collect { data ->
                                generateGestureDataUseCase.start(data)
                            }
                        } else {
                            generateGestureDataUseCase.stop()
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        receiveMessageUseCase.stop()
        generateGestureDataUseCase.stop()
        sendMessageUseCase.stop()
        job?.cancel()
        job = null
    }
}
