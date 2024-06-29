package com.example.common.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Parcelize
@Serializable
data class PerformedGesture(
    val time: Long, val gesture: GestureData, val result: GestureResult
) : Parcelable, JsonSerializable {
    override fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): PerformedGesture = Json.decodeFromString(json)
    }
}

@Parcelize
@Serializable
enum class GestureResult : Parcelable {
    Completed,
    Cancelled,
    TimeOut,
}
