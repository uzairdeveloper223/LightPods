package uzair.lightpods.android.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    downloadProgress: DownloadProgress,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDownloading =
        downloadProgress.state ==
            DownloadState.DOWNLOADING
    val isCompleted =
        downloadProgress.state ==
            DownloadState.COMPLETED
    val isFailed =
        downloadProgress.state ==
            DownloadState.FAILED
    val hasUpdate = updateInfo.isUpdateAvailable

    val animatedProgress by animateFloatAsState(
        targetValue = downloadProgress.percent,
        animationSpec = tween(300),
        label = "progress"
    )

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        shape = RoundedCornerShape(28.dp),
        containerColor =
            MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = when {
                    isCompleted -> "Installing…"
                    isDownloading -> "Downloading"
                    isFailed -> "Download Failed"
                    hasUpdate -> "Update Available"
                    else -> "LightPods is up to date"
                },
                style = MaterialTheme
                    .typography.headlineSmall
                    .copy(
                        fontWeight = FontWeight.Bold
                    )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                // ── Version info ──
                if (!isDownloading && !isCompleted) {
                    Text(
                        text = if (hasUpdate) {
                            "v${updateInfo.latestVersion} is available"
                        } else {
                            "No newer release is available right now."
                        },
                        style = MaterialTheme
                            .typography.bodyMedium,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    )
                }

                // ── Progress bar ──
                AnimatedVisibility(
                    visible = isDownloading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = {
                                animatedProgress
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(
                                    RoundedCornerShape(
                                        4.dp
                                    )
                                ),
                            color = MaterialTheme
                                .colorScheme.primary,
                            trackColor = MaterialTheme
                                .colorScheme
                                .surfaceVariant
                        )
                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement
                                    .SpaceBetween
                        ) {
                            Text(
                                text = formatBytes(
                                    downloadProgress
                                        .bytesDownloaded
                                ),
                                style = MaterialTheme
                                    .typography
                                    .labelSmall,
                                color = MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                            )
                            Text(
                                text = "${
                                    (animatedProgress * 100)
                                        .toInt()
                                }%",
                                style = MaterialTheme
                                    .typography
                                    .labelSmall
                                    .copy(
                                        fontWeight =
                                            FontWeight
                                                .Bold
                                    ),
                                color = MaterialTheme
                                    .colorScheme
                                    .primary
                            )
                        }
                    }
                }

                // ── Completed ──
                AnimatedVisibility(
                    visible = isCompleted
                ) {
                    Row(
                        verticalAlignment =
                            Alignment
                                .CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector =
                                Icons.Rounded
                                    .CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme
                                .colorScheme.primary,
                            modifier =
                                Modifier.size(20.dp)
                        )
                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )
                        Text(
                            "Opening installer…",
                            style = MaterialTheme
                                .typography
                                .bodyMedium,
                            color = MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        )
                    }
                }

                // ── Failed ──
                AnimatedVisibility(
                    visible = isFailed
                ) {
                    Row(
                        verticalAlignment =
                            Alignment
                                .CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector =
                                Icons.Rounded
                                    .Warning,
                            contentDescription = null,
                            tint = MaterialTheme
                                .colorScheme.error,
                            modifier =
                                Modifier.size(20.dp)
                        )
                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )
                        Text(
                            "Download failed." +
                                " Check connection.",
                            style = MaterialTheme
                                .typography
                                .bodyMedium,
                            color = MaterialTheme
                                .colorScheme.error
                        )
                    }
                }

                // ── Changelog ──
                if (!isDownloading &&
                    !isCompleted &&
                    updateInfo.releaseNotes
                        .isNotBlank()
                ) {
                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )
                    Text(
                        text = if (hasUpdate) {
                            "What's New"
                        } else {
                            "Details"
                        },
                        style = MaterialTheme
                            .typography.titleSmall
                            .copy(
                                fontWeight =
                                    FontWeight.Bold
                            ),
                        color = MaterialTheme
                            .colorScheme.onSurface
                    )
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                    FormattedChangelog(
                        updateInfo.releaseNotes
                    )
                }
            }
        },
        confirmButton = {
            if (!isDownloading && !isCompleted) {
                if (hasUpdate) {
                    Button(
                        onClick = onUpdate,
                        shape =
                            RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (isFailed) "Retry"
                            else "Download & Install"
                        )
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        shape =
                            RoundedCornerShape(14.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        },
        dismissButton = {
            if (!isDownloading &&
                !isCompleted &&
                hasUpdate
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape =
                        RoundedCornerShape(14.dp)
                ) {
                    Text("Later")
                }
            }
        }
    )
}

/**
 * Parses GitHub-style markdown release notes
 * into formatted annotated text.
 * Supports: ## headers, - bullet points,
 * **bold**, and plain lines.
 */
@Composable
private fun FormattedChangelog(raw: String) {
    val lines = raw
        .replace("\r\n", "\n")
        .split("\n")
        .filter { it.isNotBlank() }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        for (line in lines) {
            val trimmed = line.trim()
            when {
                // ## Section header
                trimmed.startsWith("## ") -> {
                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )
                    Text(
                        text = trimmed
                            .removePrefix("## "),
                        style = MaterialTheme
                            .typography.labelLarge
                            .copy(
                                fontWeight =
                                    FontWeight
                                        .SemiBold,
                                fontSize = 14.sp
                            ),
                        color = MaterialTheme
                            .colorScheme.primary
                    )
                }
                // # Main header
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed
                            .removePrefix("# "),
                        style = MaterialTheme
                            .typography.titleSmall
                            .copy(
                                fontWeight =
                                    FontWeight.Bold
                            ),
                        color = MaterialTheme
                            .colorScheme.onSurface
                    )
                }
                // - or * Bullet point
                trimmed.startsWith("- ") ||
                    trimmed.startsWith("* ") -> {
                    val content = trimmed
                        .removePrefix("- ")
                        .removePrefix("* ")
                    Text(
                        text = parseBoldText(
                            "  •  $content"
                        ),
                        style = MaterialTheme
                            .typography.bodySmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
                // Plain line
                else -> {
                    Text(
                        text = parseBoldText(
                            trimmed
                        ),
                        style = MaterialTheme
                            .typography.bodySmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Parses **bold** markers in text into
 * annotated string with bold spans.
 */
@Composable
private fun parseBoldText(
    text: String
) = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            // Odd segments are bold
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme
                        .colorScheme.onSurface
                )
            ) { append(part) }
        } else {
            append(part)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024f
    val mb = kb / 1024f
    return when {
        mb >= 1f -> "%.1f MB".format(mb)
        kb >= 1f -> "%.0f KB".format(kb)
        else -> "$bytes B"
    }
}
