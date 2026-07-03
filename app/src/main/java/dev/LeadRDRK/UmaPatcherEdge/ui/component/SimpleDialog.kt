package dev.LeadRDRK.UmaPatcherEdge.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.LeadRDRK.UmaPatcherEdge.R

@Composable
fun SimpleOkCancelDialog(
    title: String,
    onClose: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = { onClose(false) },
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.Column {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = { onClose(true) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onClose(false) }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}