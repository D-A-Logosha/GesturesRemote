package com.example.appclient.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appclient.R
import com.example.appclient.data.websocket.KtorWebSocketClient
import com.example.appclient.ui.theme.GesturesRemoteTheme
import com.example.settings.FakeSettingsRepository
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: ClientViewModel = koinViewModel(),
) {
    var showDialog by remember { mutableStateOf(false) }

    val buttonWidth = 0.5f * minOf(
        LocalConfiguration.current.screenWidthDp, LocalConfiguration.current.screenHeightDp
    )

    val snackbarHostState = remember { SnackbarHostState() }

    GesturesRemoteTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState, snackbar = { data ->
                    CustomSnackbar(snackbarData = data)
                })
            }, modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.app_name))
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.width(buttonWidth.dp),
                ) {
                    Text(text = "Config")
                }
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier.width(buttonWidth.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = { viewModel.onStartPauseClick() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.clientUiState.clientState != ClientState.Starting && viewModel.clientUiState.clientState != ClientState.Stopping
                    ) {
                        Text(
                            text = when (viewModel.clientUiState.clientState) {
                                ClientState.Started -> "Pause"
                                else -> "Start"
                            }
                        )
                    }
                    if (viewModel.clientUiState.clientState == ClientState.Starting || viewModel.clientUiState.clientState == ClientState.Stopping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }

                LaunchedEffect(key1 = viewModel, key2 = snackbarHostState) {
                    viewModel.snackbarMessage.collect {
                        launch {
                            snackbarHostState.showSnackbar(it)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        SettingsDialog(initialIpAddress = viewModel.clientUiState.ipAddress,
            initialPort = viewModel.clientUiState.port,
            onSettingsConfirm = { newIpAddress, newPort ->
                viewModel.onSaveSettings(newIpAddress = newIpAddress, newPort = newPort)
            },
            onDismissRequest = { showDialog = false })
    }
}

@Composable
fun CustomSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black.copy(alpha = 0.50f),
    contentColor: Color = Color.White,
) {
    Surface(
        modifier = modifier
            .wrapContentSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snackbarData.visuals.message, style = MaterialTheme.typography.bodyMedium
            )
        }
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