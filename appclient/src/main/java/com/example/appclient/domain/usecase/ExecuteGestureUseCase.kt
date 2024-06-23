package com.example.appclient.domain.usecase

import android.util.Log
import com.example.appclient.domain.interfaces.GestureServiceManager
import com.example.common.domain.GestureData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Factory
class ExecuteGestureUseCase(
    private val viewModelScope: CoroutineScope
) : KoinComponent {
    private val gestureServiceManager: GestureServiceManager by inject()

    private var job: Job? = null

    fun start(gestureDataFlow: kotlinx.coroutines.flow.SharedFlow<GestureData>) {
        job = viewModelScope.launch(Dispatchers.IO) {
            gestureDataFlow.collect { gestureData ->
                try {
                    gestureServiceManager.performSwipe(gestureData)
                    // Log.d("ExecuteGestureUseCase", "Executed gesture: $gestureData")
                } catch (e: Exception) {
                    Log.e("ExecuteGestureUseCase", "Error executing gesture: ${e.message}", e)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
