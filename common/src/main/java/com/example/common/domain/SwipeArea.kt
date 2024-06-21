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
    return Rect(this)
}

fun SwipeArea.fromJson(json: String): SwipeArea {
    val serializableSwipeArea = Json.decodeFromString<SerializableSwipeArea>(json)
    return serializableSwipeArea()
}

fun SwipeArea.toJson(): String {
    return Json.encodeToString(SerializableSwipeArea(this))
}
