package com.example.common.domain

import android.graphics.Rect
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class SerializableSwipeArea(
    val left: Int, val top: Int, val right: Int, val bottom: Int
) : Parcelable {
    constructor(swipeArea: SwipeArea) : this(
        swipeArea.left, swipeArea.top, swipeArea.right, swipeArea.bottom
    )

    operator fun invoke(): Rect = Rect(left, top, right, bottom)

    operator fun Rect.invoke(): SerializableSwipeArea = SerializableSwipeArea(left, top, right, bottom)

}
