package com.example.appserver.ui

data class ServerUiState (
    val isServerRun: Boolean = false,
    val port: String = "8081"
)