package com.example.appclient.domain.usecase

import android.util.Log
import com.example.appclient.data.websocket.WebSocketClient
import com.example.common.domain.JsonSerializable
import com.example.common.domain.SerializableSwipeArea
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
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

    fun <T> start(dataFlow: SharedFlow<T>): Job {
        var job = jobMap[dataFlow]
        if (job != null && job.isActive) {
            return job
        }
        job = useCaseScope.launch(Dispatchers.IO) {
            dataFlow.collect { data ->
                try {
                    val message = when (data) {
                        is String -> data
                        is SwipeArea -> SerializableSwipeArea(data).toJson()
                        is JsonSerializable -> data.toJson()
                        else -> {
                            throw IllegalArgumentException("Data type ${data?.let { it::class.simpleName }} does not implement JsonSerializable")
                        }
                    }
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
        val job = jobMap[dataFlow]
        job?.cancel()
        jobMap[dataFlow] = null
    }

    fun stop() {
        jobMap.forEach { (flow, job) ->
            job?.cancel()
            jobMap[flow] = null
        }
    }
}
