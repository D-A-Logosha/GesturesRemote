package com.example.common.domain

import android.graphics.Rect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

typealias SwipeArea = Rect

fun Rect.copy(
    left: Int = this.left,
    top: Int = this.top,
    right: Int = this.right,
    bottom: Int = this.bottom,
): Rect {
    return Rect(left, top, right, bottom)
}

fun SwipeArea.toJson(): String = Json.encodeToString(SerializableSwipeArea(this))

fun SwipeArea.fromJson(json: String) = Json.decodeFromString<SerializableSwipeArea>(json)

fun SwipeArea.shrink(fraction: Float): SwipeArea {
    require(fraction in 0f..1f) { "Fraction must be between 0 and 1" }
    val factor = fraction / 2
    val left: Int = if (this.left > 0) this.left else 0
    val top: Int = if (this.top > 0) this.top else 0
    val bottom: Int = if (this.bottom > 0) this.bottom else 0
    val right: Int = if (this.right > 0) this.right else 0
    return copy(
        left = (left + (right - left) * factor).toInt(),
        top = (top + (bottom - top) * factor).toInt(),
        bottom = (bottom - (bottom - top) * factor).toInt(),
        right = (right - (right - left) * factor).toInt()
    )
}
