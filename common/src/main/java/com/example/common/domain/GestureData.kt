package com.example.common.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Parcelize
@Serializable
data class GestureData(
    val start: Point,
    val end: Point,
    val duration: Long,
) : Parcelable, JsonSerializable {
    constructor(gesture: GestureData) : this(
        Point(gesture.start.x, gesture.start.y),
        Point(gesture.end.x, gesture.end.y),
        gesture.duration,
    )

    override fun toJson(): String = Json.encodeToString(this)

    companion object {
         fun fromJson(json: String): GestureData = Json.decodeFromString(json)
    }
}

@Parcelize
@Serializable
data class Point(
    val x: Int,
    val y: Int,
) : Parcelable
