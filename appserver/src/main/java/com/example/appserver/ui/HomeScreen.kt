package com.example.appserver.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appserver.R
import com.example.appserver.data.KtorWebSocketServer
import com.example.appserver.ui.theme.GesturesRemoteTheme
import com.example.settings.FakeSettingsRepository

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: ServerViewModel,
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.app_name))
        Spacer(modifier = Modifier.height(8.dp))
        val (serverStatus, statusColor) = when (viewModel.serverUiState.isServerRun) {
            true -> "Server is running" to Color.Green
            false -> "Server is stopped" to Color.Red
        }
        Text(
            text = serverStatus,
            color = statusColor,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showDialog = true }) {
            Text("Config")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.onStartClick() }) {
            Text("Start")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.onStopClick() }) {
            Text("Stop")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.onLogsClick() }) {
            Text("Logs")
        }
    }

    if (showDialog) {
        SettingsDialog(
            initialPort = viewModel.serverUiState.port,
            onSettingsConfirm = { newPort ->
                viewModel.onSaveSettings(newPort)
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
            viewModel = ServerViewModel(
                settingsRepository = FakeSettingsRepository(),
                webSocketServer = KtorWebSocketServer(),
            )
        )
    }
}