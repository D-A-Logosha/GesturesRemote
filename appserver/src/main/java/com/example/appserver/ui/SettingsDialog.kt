package com.example.appserver.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.appserver.ui.theme.GesturesRemoteTheme
import com.example.settings.ValidationUtils

@Composable
fun SettingsDialog(
    initialPort: String,
    onSettingsConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var port by remember { mutableStateOf(initialPort) }
    var isPortValid by remember { mutableStateOf(ValidationUtils.isValidPort(port)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Settings") },
        text = {
            Column {
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
                if (isPortValid) {
                    onSettingsConfirm(port)
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
fun ServerSettingsDialogPreview() {
    GesturesRemoteTheme {
        SettingsDialog(
            initialPort = "8080",
            onSettingsConfirm = { _ -> },
            onDismissRequest = { }
        )
    }
}