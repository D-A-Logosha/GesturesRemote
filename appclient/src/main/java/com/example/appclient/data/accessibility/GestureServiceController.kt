package com.example.appclient.data.accessibility

import com.example.appclient.domain.GestureServiceHandler
import com.example.appclient.domain.GestureServiceManager
import com.example.common.domain.GestureData
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Single
class GestureServiceController : GestureServiceManager, GestureServiceHandler {

    private val _isServiceEnabled = MutableSharedFlow<Boolean>(replay = 1)
    override val isServiceEnabled: SharedFlow<Boolean> = _isServiceEnabled.asSharedFlow()
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
        coroutineScope.launch(Dispatchers.IO) {
            _isServiceEnabled.emit(isEnabled)
        }
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
}
