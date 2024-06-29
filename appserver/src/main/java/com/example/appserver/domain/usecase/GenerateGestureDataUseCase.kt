package com.example.appserver.domain.usecase

import android.util.Log
import com.example.appserver.BuildConfig
import com.example.common.domain.GestureData
import com.example.common.domain.Point
import com.example.common.domain.SwipeArea
import com.example.common.domain.shrink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlin.random.Random

class GenerateGestureDataUseCase(
    private val useCaseScope: CoroutineScope,
) : KoinComponent {

    private val _gestureFlow = MutableSharedFlow<GestureData>(replay = 0)
    val gestureFlow = _gestureFlow.asSharedFlow()

    private var job: Job? = null

    private var swipeAreaLocal: SwipeArea = SwipeArea()

    @Synchronized
    fun start(swipeArea: SwipeArea) {
        if (BuildConfig.LOG_LVL>8) Log.d("GGD.UseCase","Starting: $swipeArea")
        swipeAreaLocal = swipeArea.shrink(0.2f)
        job?.let { return }
        job = useCaseScope.launch(Dispatchers.IO) {
            var isSwipeDown = true
            while (isActive) {
                if (swipeAreaLocal.width() == 0 || swipeAreaLocal.height() == 0) {
                    if (BuildConfig.LOG_LVL>7) Log.d(
                        "GGD.UseCase",
                        "swipe area is empty: $swipeAreaLocal"
                    )
                    delay(2222L)
                    continue
                }
                val x = Random.nextInt(swipeAreaLocal.left, swipeAreaLocal.right)
                val gestureData = if (isSwipeDown) {
                    val topY = Random.nextInt(swipeAreaLocal.top, swipeAreaLocal.bottom)
                    GestureData(
                        start = Point(x, topY),
                        end = Point(x, Random.nextInt(topY, swipeAreaLocal.bottom)),
                        duration = Random.nextLong(256, 1024)
                    )
                } else {
                    val topY = Random.nextInt(swipeAreaLocal.top, swipeAreaLocal.bottom)
                    GestureData(
                        start = Point(x, Random.nextInt(topY, swipeAreaLocal.bottom)),
                        end = Point(x, topY),
                        duration = Random.nextLong(256, 1024)
                    )
                }
                _gestureFlow.emit(gestureData)
                if (BuildConfig.LOG_LVL>7) Log.d("GGD.UseCase", "Generated gesture: $gestureData")
                isSwipeDown = !isSwipeDown
                delay(Random.nextLong(2222, 4444))
            }
        }
        if (BuildConfig.LOG_LVL>8) Log.d("GGD.UseCase","Started")
    }

    fun stop() {
        job?.cancel()
        job = null
        if (BuildConfig.LOG_LVL>8) Log.d("GGD.UseCase","Stopped")
    }
}
