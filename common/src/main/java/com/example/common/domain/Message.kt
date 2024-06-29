package com.example.common.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
class Message private constructor(
    val type: String,
    val data: JsonElement,
) {

    constructor(swipeArea: SwipeArea) : this(
        type = "swipeArea",
        data = Json.encodeToJsonElement(SerializableSwipeArea(swipeArea)),
    )

    constructor(performedGesture: PerformedGesture) : this(
        type = "performedGesture",
        data = Json.encodeToJsonElement(performedGesture),
    )
}
