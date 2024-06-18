package com.example.appclient.ui

import androidx.compose.runtime.State
import kotlinx.coroutines.flow.SharedFlow

interface ClientViewModelInterface {
    val clientUiState: ClientUiState
    val snackbarMessage: SharedFlow<String>
    fun onConfigClick()
    fun onStartPauseClick()
    fun onSaveSettings(newIpAddress: String, newPort: String)
}
