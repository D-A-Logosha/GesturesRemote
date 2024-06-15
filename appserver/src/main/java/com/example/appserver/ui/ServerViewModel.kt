package com.example.appserver.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ServerViewModel: ViewModel() {

    var serverUiState by mutableStateOf(ServerUiState())
        private set

    fun onConfigClick() {}

    fun onStartClick() {
        serverUiState = serverUiState.copy(isServerRun = true)
    }

    fun onStopClick() {
        serverUiState = serverUiState.copy(isServerRun = false)
    }

    fun onLogsClick() {}
}