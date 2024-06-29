package com.example.appserver.domain

import kotlinx.datetime.Instant

data class Event(
    val id: Int,
    val timestamp: Instant,
    val eventType: EventType,
    val clientId: String? = null,
    val details: String? = null
)
