package com.example.appclient.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appclient.R
import com.example.appclient.data.KtorWebSocketClient
import com.example.appclient.ui.theme.GesturesRemoteTheme
import com.example.settings.FakeSettingsRepository
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: ClientViewModel = koinViewModel(),
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.app_name))
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { showDialog = true }) {
            Text(text = "Config")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { viewModel.onStartPauseClick() }) {
            val buttonText =
                if (viewModel.clientUiState.isClientRun) "Pause"
                else "Start"
            Text(text = buttonText)
        }
    }

    if (showDialog) {
        SettingsDialog(
            initialIpAddress = viewModel.clientUiState.ipAddress,
            initialPort = viewModel.clientUiState.port,
            onSettingsConfirm = { newIpAddress, newPort ->
                viewModel.onSaveSettings(newIpAddress = newIpAddress, newPort = newPort)
            },
            onDismissRequest = { showDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GesturesRemoteTheme {
        HomeScreen(
            viewModel = ClientViewModel(
                settingsRepository = FakeSettingsRepository(),
                webSocketClient = KtorWebSocketClient()
            )
        )
    }
}