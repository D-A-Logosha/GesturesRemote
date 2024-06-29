package com.example.appclient.ui

data class ClientUiState(
    val clientState: ClientState = ClientState.Stopped,
    val ipAddress: String = "10.0.2.2",
    val port: String = "8080"
)

enum class ClientState {
    Starting,
    Started,
    Stopping,
    Stopped,
}