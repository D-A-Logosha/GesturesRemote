package com.example.common.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PerformedGesture(
    val time: Long, val gesture: GestureData, val result: GestureResult
) : Parcelable

@Parcelize
@Serializable
enum class GestureResult : Parcelable {
    Completed,
    Cancelled,
    TimeOut,
}
