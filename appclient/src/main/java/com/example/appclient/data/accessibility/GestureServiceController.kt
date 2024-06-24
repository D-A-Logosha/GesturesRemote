package com.example.appclient.data.accessibility

import com.example.appclient.domain.interfaces.ChromeSwipeAreaProvider
import com.example.appclient.domain.interfaces.GestureServiceHandler
import com.example.appclient.domain.interfaces.GestureServiceManager
import com.example.appclient.domain.interfaces.PerformedGesturesProvider
import com.example.common.domain.GestureData
import com.example.common.domain.PerformedGesture
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Single
class GestureServiceController : GestureServiceManager, GestureServiceHandler,
    ChromeSwipeAreaProvider, PerformedGesturesProvider {

    override val onServiceState = MutableStateFlow(false)
    override val isServiceEnabled = onServiceState.asStateFlow()

    override val onChromeVisibleToUser = MutableStateFlow(false)
    override val isChromeVisibleToUser = onChromeVisibleToUser.asStateFlow()

    override val onChromeFocused = MutableStateFlow(false)
    override val isChromeFocused = onChromeFocused.asStateFlow()

    private val _swipeCommand = MutableSharedFlow<GestureData>(replay = 0)
    override val swipeCommand = _swipeCommand.asSharedFlow()

    private val _openChrome = MutableSharedFlow<Unit>(replay = 0)
    override val openChrome = _openChrome.asSharedFlow()

    override val openChromeResult = MutableSharedFlow<Boolean>(replay = 0)

    private val coroutineScope = CoroutineScope(SupervisorJob())

    override fun performSwipe(gesture: GestureData) {
        coroutineScope.launch(Dispatchers.IO) {
            _swipeCommand.emit(gesture)
        }
    }

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

    override val onChromeSwipeArea = MutableStateFlow(SwipeArea())
    override val chromeSwipeArea = onChromeSwipeArea.asSharedFlow()

    override val onPerformedGestures= MutableSharedFlow<PerformedGesture>(replay = 0)
    override val performedGestures = onPerformedGestures.asSharedFlow()
}
