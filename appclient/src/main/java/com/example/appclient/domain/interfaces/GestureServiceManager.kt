package com.example.appclient.domain.interfaces

import com.example.common.domain.GestureData
import kotlinx.coroutines.flow.StateFlow

interface GestureServiceManager : GestureServiceHandler {
    fun performSwipe(gesture: GestureData)
    val isServiceEnabled: StateFlow<Boolean>
    val isChromeVisibleToUser: StateFlow<Boolean>
    val isChromeFocused: StateFlow<Boolean>
    suspend fun openChrome(): Boolean
}
