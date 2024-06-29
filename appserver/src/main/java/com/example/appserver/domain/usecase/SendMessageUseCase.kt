package com.example.appserver.domain.usecase

import android.util.Log
import com.example.appserver.BuildConfig
import com.example.appserver.data.websocket.WebSocketServer
import com.example.common.domain.GestureData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SendMessageUseCase(
    private val clientId: String,
    private val useCaseScope: CoroutineScope,
) : KoinComponent {
    private val webSocketServer: WebSocketServer by inject()

    private var jobMap: MutableMap<SharedFlow<GestureData>, Job?> = mutableMapOf()

    @Synchronized
    fun start(gestureDataFlow: SharedFlow<GestureData>): Job {
        if (BuildConfig.LOG_LVL>8) Log.d("SM.UseCase", "Try starting:$clientId")
        var job = jobMap[gestureDataFlow]
        if (job != null && job.isActive) {
            return job
        }
        job = useCaseScope.launch(Dispatchers.IO) {
            gestureDataFlow.collect { data ->
                try {
                    if (BuildConfig.LOG_LVL>7) Log.d("SM.UseCase", "Sending message:$clientId: ${data.toJson()}")
                    webSocketServer.send(clientId, data.toJson())
                } catch (e: Exception) {
                    if (BuildConfig.LOG_LVL>3) Log.e("SM.UseCase", "Error send message:$clientId: ${e.message}", e)
                }
            }
        }
        jobMap[gestureDataFlow] = job
        if (BuildConfig.LOG_LVL>8) Log.d("SM.UseCase", "Started:$clientId")
        return job
    }

    fun stop() {
        jobMap.forEach { (_, job) ->
            job?.cancel()
        }
        jobMap.clear()
        if (BuildConfig.LOG_LVL>8) Log.d("SM.UseCase", "Stopped:$clientId")
    }
}
