package com.example.appclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    var ipAddress by rememberSaveable { mutableStateOf(initialIpAddress) }
    var port by rememberSaveable { mutableStateOf(initialPort) }
    var isIpAddressValid by rememberSaveable { mutableStateOf(ValidationUtils.isValidIpAddress(ipAddress)) }
    var isPortValid by rememberSaveable { mutableStateOf(ValidationUtils.isValidPort(port)) }

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