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
