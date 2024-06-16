package com.example.appclient.ui

data class ClientUiState (
    val isClientRun: Boolean = false,
    val ipAddress: String = "10.0.2.2",
    val port: String = "8080"
)