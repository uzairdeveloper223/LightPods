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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Update Dialog ───────────────────────────────────────────────────

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    downloadProgress: DownloadProgress,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDownloading = downloadProgress.state == DownloadState.DOWNLOADING
    val isCompleted   = downloadProgress.state == DownloadState.COMPLETED
    val isFailed      = downloadProgress.state == DownloadState.FAILED
    val hasUpdate     = updateInfo.isUpdateAvailable

    // Snap to 1f when completed so the bar always reaches the end
    val progressTarget = if (isCompleted) 1f else downloadProgress.percent
    val animatedProgress by animateFloatAsState(
        targetValue   = progressTarget,
        animationSpec = tween(durationMillis = 300),
        label         = "download_progress"
    )

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        shape            = RoundedCornerShape(28.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = when {
                    isCompleted   -> "Installing…"
                    isDownloading -> "Downloading"
                    isFailed      -> "Download Failed"
                    hasUpdate     -> "Update Available"
                    else          -> "LightPods is up to date"
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Version label (idle / failed only) ──────────
                if (!isDownloading && !isCompleted) {
                    Text(
                        text = if (hasUpdate)
                            "v${updateInfo.latestVersion} is available"
                        else
                            "No newer release is available right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Progress block ──────────────────────────────
                AnimatedVisibility(
                    visible = isDownloading || isCompleted,
                    enter   = fadeIn(),
                    exit    = fadeOut(tween(400))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        val percentInt = (animatedProgress * 100).toInt()

                        LinearProgressIndicator(
                            progress   = { animatedProgress },
                            modifier   = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .semantics {
                                    contentDescription = "Download progress: $percentInt percent"
                                },
                            color      = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text  = if (isCompleted) "Done"
                                        else formatBytes(downloadProgress.bytesDownloaded),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text  = "$percentInt%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Completed sub-row — inside the same AnimatedVisibility
                        // to avoid layout jumps or competing transitions
                        if (isCompleted) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector        = Icons.Rounded.CheckCircle,
                                    contentDescription = "Download complete",
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text  = "Opening installer…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Failed ──────────────────────────────────────
                AnimatedVisibility(
                    visible = isFailed,
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.Warning,
                            contentDescription = "Download failed",
                            tint               = MaterialTheme.colorScheme.error,
                            modifier           = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text  = "Download failed. Check your connection and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // ── Changelog ───────────────────────────────────
                if (!isDownloading && !isCompleted && updateInfo.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text  = if (hasUpdate) "What's New" else "Details",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    FormattedChangelog(updateInfo.releaseNotes)
                }
            }
        },
        confirmButton = {
            if (!isDownloading && !isCompleted) {
                if (hasUpdate) {
                    Button(
                        onClick = onUpdate,
                        shape   = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (isFailed) "Retry" else "Download & Install")
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        shape   = RoundedCornerShape(14.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        },
        dismissButton = {
            if (!isDownloading && !isCompleted && hasUpdate) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape   = RoundedCornerShape(14.dp)
                ) {
                    Text("Later")
                }
            }
        }
    )
}

// ── Changelog renderer ──────────────────────────────────────────────

@Composable
private fun FormattedChangelog(raw: String) {
    val lines = raw
        .replace("\r\n", "\n")
        .split("\n")
        .filter { it.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            when {
                // ### Sub-sub-heading  (must be checked BEFORE ## and #)
                trimmed.startsWith("### ") -> {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = parseBoldText(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // ## Sub-heading
                trimmed.startsWith("## ") -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = parseBoldText(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // # Heading
                trimmed.startsWith("# ") -> {
                    Text(
                        text  = parseBoldText(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Bullet points
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val content = trimmed.removePrefix("- ").removePrefix("* ")
                    Text(
                        text       = parseBoldText("  •  $content"),
                        style      = MaterialTheme.typography.bodySmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                // Numbered list (e.g. "1. First item")
                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val content = trimmed.replaceFirst(Regex("^\\d+\\.\\s+"), "")
                    val number  = trimmed.substringBefore(".")
                    Text(
                        text       = parseBoldText("  $number.  $content"),
                        style      = MaterialTheme.typography.bodySmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                // Blockquote
                trimmed.startsWith("> ") -> {
                    val content = trimmed.removePrefix("> ")
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text       = parseBoldText(content),
                            style      = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Horizontal rule
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Plain text
                else -> {
                    Text(
                        text       = parseBoldText(trimmed),
                        style      = MaterialTheme.typography.bodySmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Parses inline markdown: **bold** and `code` spans.
 * Returns a styled [AnnotatedString].
 */
@Composable
private fun parseBoldText(text: String): AnnotatedString = buildAnnotatedString {
    // Pattern matches **bold** or `code` spans
    val pattern = Regex("""\*\*(.+?)\*\*|`(.+?)`""")
    var lastEnd = 0

    for (match in pattern.findAll(text)) {
        // Append plain text before this match
        append(text.substring(lastEnd, match.range.first))

        val boldGroup = match.groupValues[1]   // **bold**
        val codeGroup = match.groupValues[2]    // `code`

        if (boldGroup.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            ) { append(boldGroup) }
        } else if (codeGroup.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    color = MaterialTheme.colorScheme.primary
                )
            ) { append(codeGroup) }
        }

        lastEnd = match.range.last + 1
    }

    // Append any remaining text after the last match
    if (lastEnd < text.length) {
        append(text.substring(lastEnd))
    }
}

/**
 * Formats a byte count into a human-readable string (B / KB / MB).
 */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024f
    val mb = kb / 1024f
    return when {
        mb >= 1f -> "%.1f MB".format(mb)
        kb >= 1f -> "%.0f KB".format(kb)
        else     -> "$bytes B"
    }
}