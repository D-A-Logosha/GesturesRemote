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
) : Parcelable {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): GestureData = Json.decodeFromString(json)
    }
}

@Parcelize
@Serializable
public class Point(
    val x: Int,
    val y: Int,
) : Parcelable
