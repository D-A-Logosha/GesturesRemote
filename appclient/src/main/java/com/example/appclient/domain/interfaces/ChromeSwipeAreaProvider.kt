package com.example.appclient.domain.interfaces

import com.example.common.domain.SwipeArea
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ChromeSwipeAreaProvider {
    val chromeSwipeArea: SharedFlow<SwipeArea>
    val isServiceEnabled: StateFlow<Boolean>
}
