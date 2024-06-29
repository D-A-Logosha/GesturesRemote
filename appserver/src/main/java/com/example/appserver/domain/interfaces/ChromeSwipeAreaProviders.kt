package com.example.appserver.domain.interfaces

import com.example.common.domain.SwipeArea
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ChromeSwipeAreaProviders {
    val chromeSwipeArea: SharedFlow<SwipeArea>
    val isProviderAvailable: StateFlow<Boolean>
}
