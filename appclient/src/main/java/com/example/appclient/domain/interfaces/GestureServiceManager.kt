package com.example.appclient.domain.interfaces

import com.example.common.domain.GestureData
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GestureServiceManager : GestureServiceHandler {
    fun performSwipe(gesture: GestureData)
    fun isServiceEnabled(): Boolean
    val isServiceEnabled: StateFlow<Boolean>
    fun isChromeVisibleToUser(): Boolean
    val isChromeVisibleToUser: SharedFlow<Boolean>
    fun isChromeFocused(): Boolean
    val isChromeFocused: SharedFlow<Boolean>
    fun getChromeSwipeArea(): SwipeArea
    val chromeSwipeArea: SharedFlow<SwipeArea>
    suspend fun openChrome(): Boolean
}
