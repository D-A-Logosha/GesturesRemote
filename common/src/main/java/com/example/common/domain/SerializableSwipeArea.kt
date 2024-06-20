package com.example.common.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class SerializableSwipeArea(
    val left: Int, val top: Int, val right: Int, val bottom: Int
) : Parcelable {
    constructor(swipeArea: SwipeArea) : this(
        swipeArea.left, swipeArea.top, swipeArea.right, swipeArea.bottom
    )
}
