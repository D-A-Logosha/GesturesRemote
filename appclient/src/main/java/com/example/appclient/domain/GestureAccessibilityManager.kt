package com.example.appclient.domain

interface GestureAccessibilityManager {
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long)
    fun isServiceEnabled(): Boolean
}
