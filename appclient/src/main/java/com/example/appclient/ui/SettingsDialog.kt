package com.example.appclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appclient.ui.theme.GesturesRemoteTheme
import com.example.settings.ValidationUtils

@Composable
fun SettingsDialog(
    initialIpAddress: String,
    initialPort: String,
    onSettingsConfirm: (String, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var ipAddress by remember { mutableStateOf(initialIpAddress) }
    var port by remember { mutableStateOf(initialPort) }
    var isIpAddressValid by remember { mutableStateOf(ValidationUtils.isValidIpAddress(ipAddress)) }
    var isPortValid by remember { mutableStateOf(ValidationUtils.isValidPort(port)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Config") },
        text = {
            Column {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = {
                        ipAddress = it
                        isIpAddressValid = ValidationUtils.isValidIpAddress(it)
                    },
                    label = { Text("IP Address") },
                    isError = !isIpAddressValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (!isIpAddressValid) {
                            Text(
                                text = "Invalid IP Address",
                                color = Color.Red
                            )
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        isPortValid = ValidationUtils.isValidPort(it)
                    },
                    label = { Text("Port") },
                    isError = !isPortValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (!isPortValid) {
                            Text(
                                text = "Invalid Port. Port must be between 1 and 65535",
                                color = Color.Red
                            )
                        }
                    },
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isIpAddressValid && isPortValid) {
                    onSettingsConfirm(ipAddress, port)
                    onDismissRequest()
                }
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