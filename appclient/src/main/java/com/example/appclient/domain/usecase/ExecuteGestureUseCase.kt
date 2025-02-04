package com.example.appclient.domain.usecase

import android.util.Log
import com.example.appclient.BuildConfig
import com.example.appclient.domain.interfaces.GestureServiceManager
import com.example.common.domain.GestureData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
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

    @Synchronized
    fun start(gestureDataFlow: SharedFlow<GestureData>) {
        job?.run { return }
        job = viewModelScope.launch(Dispatchers.IO) {
            gestureDataFlow.collect { gestureData ->
                try {
                    gestureServiceManager.performSwipe(gestureData)
                    if (BuildConfig.LOG_LVL>8) Log.d("ExecuteGestureUseCase", "Executed gesture: $gestureData")
                } catch (e: Exception) {
                    if (BuildConfig.LOG_LVL>3) Log.e("ExecuteGestureUseCase", "Error executing gesture: ${e.message}", e)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
