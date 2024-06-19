package com.example.appclient.domain

import com.example.common.domain.GestureData
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.flow.SharedFlow

interface GestureServiceManager : GestureServiceHandler {
    fun performSwipe(gesture: GestureData)
    fun isServiceEnabled(): Boolean
    val isServiceEnabled: SharedFlow<Boolean>
    fun isChromeVisibleToUser(): Boolean
    val isChromeVisibleToUser: SharedFlow<Boolean>
    fun isChromeFocused(): Boolean
    val isChromeFocused: SharedFlow<Boolean>
    fun getChromeSwipeArea(): SwipeArea
    val chromeSwipeArea: SharedFlow<SwipeArea>
    fun openChrome()
}
