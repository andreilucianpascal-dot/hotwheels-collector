package com.example.hotwheelscollectors.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FolderConfirmationDialog(
    suggestedFolder: String,
    carModel: String,
    onConfirm: () -> Unit,
    onCustomFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customFolder by remember { mutableStateOf(suggestedFolder) }
    var showCustomInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Folder Path") },
        text = {
            Column {
                if (!showCustomInput) {
                    Text("Suggested folder for $carModel:")
                    Text(suggestedFolder, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Is this correct?")
                } else {
                    OutlinedTextField(
                        value = customFolder,
                        onValueChange = { customFolder = it },
                        label = { Text("Custom Folder Path") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (!showCustomInput) {
                Button(onClick = onConfirm) {
                    Text("Yes, Use This")
                }
            } else {
                Button(onClick = { onCustomFolder(customFolder) }) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            if (!showCustomInput) {
                TextButton(onClick = { showCustomInput = true }) {
                    Text("No, Custom Path")
                }
            } else {
                TextButton(onClick = { showCustomInput = false }) {
                    Text("Cancel")
                }
            }
        }
    )
}