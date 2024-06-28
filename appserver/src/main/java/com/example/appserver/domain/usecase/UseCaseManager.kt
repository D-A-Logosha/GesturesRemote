package com.example.appserver.domain.usecase

import android.util.Log
import com.example.appserver.BuildConfig
import com.example.appserver.data.EventLogger
import com.example.appserver.data.websocket.WebSocketServer
import com.example.appserver.domain.EventType
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

    private val eventLogger: EventLogger by inject()

    private val useCaseScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())

    private var job: Job? = null

    private val receiveMessageUseCase = ReceiveMessageUseCase(clientId, useCaseScope)
    private val generateGestureDataUseCase = GenerateGestureDataUseCase(useCaseScope)
    private val sendMessageUseCase = SendMessageUseCase(clientId, useCaseScope)
    private val chromeSwipeAreaProvider = receiveMessageUseCase as ChromeSwipeAreaProviders
    private val performedGesturesProvider = receiveMessageUseCase as PerformedGesturesProviders

    @Synchronized
    fun start() {
        job?.let { return }
        job = useCaseScope.launch(Dispatchers.IO) {
            useCaseScope.launch(Dispatchers.IO) {
                webSocketServer.connectedClients.collect { connectedClients ->
                    if (connectedClients.contains(clientId)) {
                        receiveMessageUseCase.start()
                        if (BuildConfig.LOG_LVL>7) Log.d("UseCaseManager", "Receive messages started")
                    } else {
                        receiveMessageUseCase.stop()
                        if (BuildConfig.LOG_LVL>7) Log.d("UseCaseManager", "Receive messages stopped")
                    }
                }

            }
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
        eventLogger.logUseCaseManagerEvent(clientId, EventType.ManagerStarted)
    }

    fun stop() {
        receiveMessageUseCase.stop()
        generateGestureDataUseCase.stop()
        sendMessageUseCase.stop()
        job?.cancel()
        job = null
        eventLogger.logUseCaseManagerEvent(clientId, EventType.ManagerStopped)
    }
}
