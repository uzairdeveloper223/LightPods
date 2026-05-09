package uzair.lightpods.android.updater

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor =
            MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Update Available",
                style = MaterialTheme
                    .typography.headlineSmall
                    .copy(
                        fontWeight = FontWeight.Bold
                    )
            )
        },
        text = {
            Column {
                Text(
                    text = "v${updateInfo.latestVersion}" +
                        " is available" +
                        " (you have v1.0.0)",
                    style = MaterialTheme
                        .typography.bodyMedium,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant
                )

                if (updateInfo.releaseNotes
                        .isNotBlank()
                ) {
                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                    Text(
                        text = "What's new:",
                        style = MaterialTheme
                            .typography.labelLarge
                            .copy(
                                fontWeight =
                                    FontWeight.SemiBold
                            ),
                        color = MaterialTheme
                            .colorScheme.onSurface
                    )
                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )
                    Text(
                        text = updateInfo
                            .releaseNotes
                            .take(500),
                        style = MaterialTheme
                            .typography.bodySmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Download & Install")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Later")
            }
        }
    )
}
