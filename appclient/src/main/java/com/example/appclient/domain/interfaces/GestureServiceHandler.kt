package com.example.appclient.domain.interfaces

import com.example.common.domain.GestureData
import com.example.common.domain.GestureResult
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

interface GestureServiceHandler {
    val swipeCommand: SharedFlow<GestureData>
    val openChrome: SharedFlow<Unit>
    fun onServiceStateChanged(isEnabled: Boolean)
    fun onChromeVisibleToUserChanged(isChromeVisible: Boolean)
    fun onChromeFocusedChanged(isChromeFocused: Boolean)
    fun onSwipeAreaChanged(swipeArea: SwipeArea)
    val openChromeResult: MutableSharedFlow<Boolean>
    fun onGesturePerformed(timestamp: Long, gestureData: GestureData, result: GestureResult)
}