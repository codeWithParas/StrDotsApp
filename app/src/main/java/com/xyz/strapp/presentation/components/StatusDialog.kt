package com.xyz.strapp.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle // Default success icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A reusable Jetpack Compose dialog to display a success message.
 *
 * @param showDialog Controls whether the dialog is shown.
 * @param onDismissRequest Lambda to be invoked when the dialog is dismissed (e.g., by clicking outside or the dismiss button).
 * @param successIcon The vector icon to display at the top of the dialog. Defaults to a green CheckCircle.
 * @param title An optional title for the dialog.
 * @param message The main success message to display.
 * @param dismissButtonText Text for the dismiss button. Defaults to "OK".
 * @param iconTint Tint color for the success icon. Defaults to a common success green.
 */
@Composable
fun SuccessMessageDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    successIcon: ImageVector = Icons.Filled.CheckCircle,
    iconTint: Color = Color(0xFF4CAF50), // A common success green
    title: String? = "Success!",
    message: String,
    dismissButtonText: String = "OK"
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = {
                Icon(
                    imageVector = successIcon,
                    contentDescription = "Success Icon",
                    modifier = Modifier.size(48.dp),
                    tint = iconTint
                )
            },
            title = title?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Spacer if no title but icon is present, to give icon some space
                    if (title == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                        fontSize = 24.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(dismissButtonText)
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SuccessMessageDialogPreviewWithTitle() {
    MaterialTheme { // Wrap with your app's theme or MaterialTheme for preview
        SuccessMessageDialog(
            showDialog = true,
            onDismissRequest = {},
            title = "Task Completed!",
            message = "Your details have been submitted successfully and the task is now complete."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SuccessMessageDialogPreviewNoTitle() {
    MaterialTheme {
        SuccessMessageDialog(
            showDialog = true,
            onDismissRequest = {},
            title = null,
            message = "Image uploaded successfully."
        )
    }
}
