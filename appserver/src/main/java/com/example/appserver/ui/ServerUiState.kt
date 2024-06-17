package com.example.appserver.ui

data class ServerUiState (
    val serverState: ServerState = ServerState.Stopped,
    val port: String = "8081"
)

enum class ServerState {
    Starting,
    Running,
    Stopping,
    Stopped,
}