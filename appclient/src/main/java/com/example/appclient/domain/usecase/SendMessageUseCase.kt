package com.example.appclient.domain.usecase

import android.util.Log
import com.example.appclient.data.websocket.WebSocketClient
import com.example.common.domain.Message
import com.example.common.domain.PerformedGesture
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Factory
class SendMessageUseCase(
    private val useCaseScope: CoroutineScope
) : KoinComponent {
    private val webSocketClient: WebSocketClient by inject()

    private var jobMap: MutableMap<SharedFlow<*>, Job?> = mutableMapOf()

    private var job: Job? = null

    @Synchronized
    fun startSwipeArea(dataFlow: SharedFlow<SwipeArea>): Job {
        return returnJob(dataFlow) ?: run {
            startFlow(dataFlow) { data ->
                Json.encodeToString(Message(data))
            }
        }
    }

    @Synchronized
    fun startPerformedGesture(dataFlow: SharedFlow<PerformedGesture>): Job {
        return returnJob(dataFlow) ?: run {
            startFlow(dataFlow) { data ->
                Json.encodeToString(Message(data))
            }
        }
    }

    private fun <T> returnJob(dataFlow: SharedFlow<T>): Job? {
        val job = jobMap[dataFlow]
        if (job != null && job.isActive) {
            return job
        }
        return null
    }

    private fun <T> startFlow(dataFlow: SharedFlow<T>, messageBuilder: (T) -> String): Job {
        val job = useCaseScope.launch(Dispatchers.IO) {
            dataFlow.collect { data ->
                try {
                    val message = messageBuilder(data)
                    webSocketClient.send(message)
                    Log.d("SendMessageUseCase", "Sending message: $message")
                } catch (e: Exception) {
                    Log.e("SendMessageUseCase", "Error sending message: ${e.message}", e)
                }
            }
        }
        jobMap[dataFlow] = job
        return job
    }

    fun stop(dataFlow: SharedFlow<*>) {
        jobMap[dataFlow]?.cancel()
        jobMap.remove(dataFlow)
    }

    fun stop() {
        jobMap.forEach { (_, job) ->
            job?.cancel()
        }
        jobMap.clear()
    }
}
