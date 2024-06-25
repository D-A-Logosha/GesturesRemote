package com.example.appserver.domain.interfaces

import com.example.common.domain.PerformedGesture
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface PerformedGesturesProviders {
    val performedGestures: SharedFlow<PerformedGesture>
    val isProviderAvailable: StateFlow<Boolean>
}
