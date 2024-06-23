package com.example.appclient.domain.interfaces

import com.example.common.domain.PerformedGesture
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface PerformedGesturesProvider {
    val performedGesturesFlow: SharedFlow<PerformedGesture>
    val isServiceEnabled: StateFlow<Boolean>
}
