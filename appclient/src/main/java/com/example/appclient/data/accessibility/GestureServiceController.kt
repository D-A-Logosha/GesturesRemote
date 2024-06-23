package com.example.appclient.data.accessibility

import android.util.Log
import com.example.appclient.domain.interfaces.GestureServiceHandler
import com.example.appclient.domain.interfaces.GestureServiceManager
import com.example.appclient.domain.interfaces.PerformedGesturesProvider
import com.example.common.domain.GestureData
import com.example.common.domain.GestureResult
import com.example.common.domain.PerformedGesture
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Single
class GestureServiceController : GestureServiceManager, GestureServiceHandler,
    PerformedGesturesProvider {

    private val _isServiceEnabled = MutableStateFlow(false)
    override val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    private var isServiceEnabledLocal = false

    private val _isChromeVisibleToUser = MutableSharedFlow<Boolean>(replay = 1)
    override val isChromeVisibleToUser = _isChromeVisibleToUser.asSharedFlow()
    private var isChromeVisibleToUserLocal = false

    private val _isChromeFocused = MutableSharedFlow<Boolean>(replay = 1)
    override val isChromeFocused: SharedFlow<Boolean> = _isChromeFocused.asSharedFlow()
    private var isChromeFocusedLocal = false

    private val _chromeSwipeArea = MutableSharedFlow<SwipeArea>(replay = 1)
    override val chromeSwipeArea: SharedFlow<SwipeArea> = _chromeSwipeArea.asSharedFlow()
    private var chromeSwipeAreaLocal = SwipeArea()

    private val _swipeCommand = MutableSharedFlow<GestureData>(replay = 1)
    override val swipeCommand = _swipeCommand.asSharedFlow()

    private val _openChrome = MutableSharedFlow<Unit>(replay = 1)
    override val openChrome = _openChrome.asSharedFlow()

    override val openChromeResult = MutableSharedFlow<Boolean>(replay = 0)

    private val coroutineScope = CoroutineScope(SupervisorJob())

    override fun onServiceStateChanged(isEnabled: Boolean) {
        isServiceEnabledLocal = isEnabled
        _isServiceEnabled.update { isEnabled }
    }

    override fun onChromeVisibleToUserChanged(isChromeVisible: Boolean) {
        isChromeVisibleToUserLocal = isChromeVisible
        coroutineScope.launch(Dispatchers.IO) {
            _isChromeVisibleToUser.emit(isChromeVisible)
        }
    }

    override fun onChromeFocusedChanged(isChromeFocused: Boolean) {
        isChromeFocusedLocal = isChromeFocused
        coroutineScope.launch(Dispatchers.IO) {
            _isChromeFocused.emit(isChromeFocused)
        }
    }

    override fun onSwipeAreaChanged(swipeArea: SwipeArea) {
        chromeSwipeAreaLocal = swipeArea
        coroutineScope.launch(Dispatchers.IO) {
            _chromeSwipeArea.emit(swipeArea)
        }
    }

    override fun performSwipe(gesture: GestureData) {
        coroutineScope.launch(Dispatchers.IO) {
            _swipeCommand.emit(gesture)
        }
    }

    override fun isServiceEnabled() = isServiceEnabledLocal
    override fun isChromeVisibleToUser() = isChromeVisibleToUserLocal
    override fun isChromeFocused() = isChromeFocusedLocal
    override fun getChromeSwipeArea() = chromeSwipeAreaLocal


    override suspend fun openChrome(): Boolean {
        return suspendCoroutine { continuation ->
            coroutineScope.launch(Dispatchers.IO) {
                _openChrome.emit(Unit)
            }
            coroutineScope.launch(Dispatchers.IO) {
                val result = openChromeResult.first()
                continuation.resume(result)
            }
        }
    }

    private val _performedGesturesFlow = MutableSharedFlow<PerformedGesture>(replay = 0)
    override val performedGesturesFlow = _performedGesturesFlow.asSharedFlow()

    override fun onGesturePerformed(
        timestamp: Long, gestureData: GestureData, result: GestureResult
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            _performedGesturesFlow.emit(
                PerformedGesture(timestamp, gestureData, result)
            )
            Log.d(
                "PerformedGesturesProvider",
                "Gesture completed: ${PerformedGesture(timestamp, gestureData, result)}"
            )
        }
    }
}
