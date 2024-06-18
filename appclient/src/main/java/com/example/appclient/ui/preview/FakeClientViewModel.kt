package com.example.appclient.ui.preview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appclient.ui.ClientState
import com.example.appclient.ui.ClientUiState
import com.example.appclient.ui.ClientViewModelInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class FakeClientViewModel : ViewModel(), ClientViewModelInterface {

    override var clientUiState by mutableStateOf(ClientUiState())
        private set

    override var snackbarMessage = MutableSharedFlow<String>(replay = 0)
        private set

    override fun onConfigClick() {}

    override fun onStartPauseClick() {
        when (clientUiState.clientState) {

            ClientState.Starting -> {}

            ClientState.Started ->
                setClientUiStateWithDelay(
                    stateInit = clientUiState.copy(clientState = ClientState.Stopping),
                    stateEnded = clientUiState.copy(clientState = ClientState.Stopped),
                    delay = 1000L
                )

            ClientState.Stopping -> {}

            ClientState.Stopped ->
                setClientUiStateWithDelay(
                    stateInit = clientUiState.copy(clientState = ClientState.Starting),
                    stateEnded = clientUiState.copy(clientState = ClientState.Started),
                    delay = 1000L
                )
        }
    }

    private fun setClientUiStateWithDelay(
        stateInit: ClientUiState, stateEnded: ClientUiState, delay: Long
    ) {
        clientUiState = stateInit
        viewModelScope.launch(Dispatchers.IO) {
            delay(delay)
            clientUiState = stateEnded
        }
    }

    override fun onSaveSettings(newIpAddress: String, newPort: String) {
        clientUiState = clientUiState.copy(ipAddress = newIpAddress, port = newPort)
    }
}
