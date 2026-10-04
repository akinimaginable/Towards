package org.etrange.towards.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

@Composable
fun AddPlaceLabelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    labelError: (String) -> String?,
    title: String = "Add shortcut",
) {
    var labelDraft by rememberSaveable { mutableStateOf("") }
    var labelErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun confirm() {
        val error = labelError(labelDraft)
        if (error != null) {
            labelErrorMessage = error
        } else {
            onConfirm(labelDraft.trim())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = labelDraft,
                onValueChange = {
                    labelDraft = it
                    labelErrorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Name") },
                isError = labelErrorMessage != null,
                supportingText = labelErrorMessage?.let { message ->
                    { Text(message) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
            )
        },
        confirmButton = {
            TextButton(onClick = { confirm() }) {
                Text("Choose place")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
