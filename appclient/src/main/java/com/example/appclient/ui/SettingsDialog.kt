package com.example.appclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appclient.ui.theme.GesturesRemoteTheme

@Composable
fun SettingsDialog(
    initialIpAddress: String,
    initialPort: String,
    onSettingsConfirm: (String, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var ipAddress by remember { mutableStateOf(initialIpAddress) }
    var port by remember { mutableStateOf(initialPort) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Config") },
        text = {
            Column {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("IP Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSettingsConfirm(ipAddress, port)
                onDismissRequest()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsDialogPreview() {
    GesturesRemoteTheme {
        SettingsDialog(
            initialIpAddress = "10.0.2.2",
            initialPort = "8080",
            onSettingsConfirm = { _, _ -> },
            onDismissRequest = { }
        )
    }
}