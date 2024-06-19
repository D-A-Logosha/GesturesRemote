package com.example.common.domain

import android.graphics.Point

data class GestureData(
    val start: Point,
    val end: Point,
    val duration: Long,
)
