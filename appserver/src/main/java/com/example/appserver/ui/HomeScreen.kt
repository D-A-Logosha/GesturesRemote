package com.example.appserver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appserver.ui.theme.GesturesRemoteTheme

@Composable
fun HomeScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Server App")
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
        Button(onClick = { viewModel.onConfigClick() }) {
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
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GesturesRemoteTheme {
        HomeScreen(ServerViewModel())
    }
}