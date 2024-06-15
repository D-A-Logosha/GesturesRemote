package com.example.appclient.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appclient.R
import com.example.appclient.ui.theme.GesturesRemoteTheme

@Composable
fun HomeScreen(
    viewModel: ClientViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.app_name))
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { viewModel.onConfigClick() }) {
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
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GesturesRemoteTheme {
        HomeScreen(ClientViewModel())
    }
}