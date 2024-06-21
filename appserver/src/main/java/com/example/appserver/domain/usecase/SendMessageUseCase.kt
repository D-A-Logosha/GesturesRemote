package com.example.appserver.domain.usecase

import android.util.Log
import com.example.appserver.data.WebSocketServer
import com.example.common.domain.GestureData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SendMessageUseCase(
    private val viewModelScope: CoroutineScope,
) : KoinComponent {
    private val webSocketServer: WebSocketServer by inject()

    private var jobMap: MutableMap<SharedFlow<GestureData>, Job?> = mutableMapOf()

    fun start(gestureDataFlow: SharedFlow<GestureData>): Job {
        var job = jobMap[gestureDataFlow]
        if (job != null && job.isActive) {
            return job
        }
        job = viewModelScope.launch(Dispatchers.IO) {
            gestureDataFlow.collect { data ->
                try {
                    Log.d("SendMessageUseCase", "Sending message: ${data.toJson()}")
                    webSocketServer.send(data.toJson())
                } catch (e: Exception) {
                    Log.e("SendMessageUseCase", "Error sending message: ${e.message}", e)
                }
            }
        }
        jobMap[gestureDataFlow] = job
        return job
    }

    fun stop() {
        jobMap.forEach { (flow, job) ->
            job?.cancel()
            jobMap[flow] = null
        }
    }
}
