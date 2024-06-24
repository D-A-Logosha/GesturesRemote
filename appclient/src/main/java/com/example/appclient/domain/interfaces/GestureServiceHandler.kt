package com.example.appclient.domain.interfaces

import com.example.common.domain.GestureData
import com.example.common.domain.PerformedGesture
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

interface GestureServiceHandler {
    val swipeCommand: SharedFlow<GestureData>
    val openChrome: SharedFlow<Unit>
    val onServiceState: MutableStateFlow<Boolean>
    val onChromeVisibleToUser: MutableStateFlow<Boolean>
    val onChromeFocused: MutableStateFlow<Boolean>
    val onChromeSwipeArea: MutableStateFlow<SwipeArea>
    val openChromeResult: MutableSharedFlow<Boolean>
    val onPerformedGestures: MutableSharedFlow<PerformedGesture>
}
