package com.example.appclient.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ClientViewModel : ViewModel() {

    var clientUiState by mutableStateOf(ClientUiState())
        private set

    fun onConfigClick() {}

    fun onStartPauseClick() {
        clientUiState = clientUiState.copy(isClientRun = !clientUiState.isClientRun)
    }
}