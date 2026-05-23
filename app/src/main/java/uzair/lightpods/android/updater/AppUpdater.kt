package uzair.lightpods.android.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// ── Data models ─────────────────────────────────────────────────────

data class UpdateInfo(
    val latestVersion: String = "",
    val latestVersionCode: Int = 0,
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val isUpdateAvailable: Boolean = false
)

data class DownloadProgress(
    val state: DownloadState = DownloadState.IDLE,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val percent: Float = 0f
)

enum class DownloadState {
    IDLE, DOWNLOADING, COMPLETED, FAILED
}

// ── AppUpdater ──────────────────────────────────────────────────────

class AppUpdater(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdater"

        private const val GITHUB_API_URL =
            "https://api.github.com/repos/" +
                "uzairdeveloper223/lightpods/" +
                "releases/latest"

        const val CURRENT_VERSION = "1.4.3"

        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val BUFFER_SIZE = 8 * 1024   // 8 KB read buffer
        private const val UPDATES_DIR = "updates"
    }

    // Owned scope — cancelled in release()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    private var downloadJob: Job? = null

    /** Call from ViewModel.onCleared() to stop any active download. */
    fun release() {
        downloadJob?.cancel()
        scope.cancel()
    }

    /** Resets the progress state to IDLE (e.g. when dialog is dismissed). */
    fun resetProgress() {
        downloadJob?.cancel()
        _progress.value = DownloadProgress()
    }

    // ── Update check ────────────────────────────────────────────────

    suspend fun checkForUpdate(): UpdateInfo? =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(GITHUB_API_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "GitHub API returned ${conn.responseCode}")
                    return@withContext null
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseRelease(body)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed", e)
                null
            } finally {
                conn?.disconnect()
            }
        }

    // ── Download & Install ──────────────────────────────────────────

    fun downloadAndInstall(downloadUrl: String, version: String) {
        // Cancel any previous download
        downloadJob?.cancel()

        _progress.value = DownloadProgress(state = DownloadState.DOWNLOADING)

        val fileName = "LightPods-v$version.apk"

        downloadJob = scope.launch {
            var conn: HttpURLConnection? = null
            try {
                // ── Prepare destination ─────────────────────────
                val updatesDir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }

                // Delete any previous APKs in our updates cache
                cleanUpdateCache(updatesDir)

                val apkFile = File(updatesDir, fileName)

                // ── Connect ─────────────────────────────────────
                conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = true
                }

                // GitHub returns 302 → CDN, HttpURLConnection follows automatically
                val responseCode = conn.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Download HTTP $responseCode")
                    _progress.update { it.copy(state = DownloadState.FAILED) }
                    return@launch
                }

                val totalBytes = conn.contentLengthLong   // may be -1
                val input = conn.inputStream
                val output = FileOutputStream(apkFile)

                // ── Stream with progress ────────────────────────
                var bytesRead: Long = 0
                val buffer = ByteArray(BUFFER_SIZE)

                input.use { inStream ->
                    output.use { outStream ->
                        var len: Int
                        while (inStream.read(buffer).also { len = it } != -1) {
                            outStream.write(buffer, 0, len)
                            bytesRead += len

                            val pct = if (totalBytes > 0) {
                                (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                            } else 0f

                            _progress.update {
                                it.copy(
                                    bytesDownloaded = bytesRead,
                                    totalBytes = totalBytes,
                                    percent = pct
                                )
                            }
                        }
                    }
                }

                // ── Verify download ─────────────────────────────
                if (totalBytes > 0 && bytesRead < totalBytes) {
                    Log.e(TAG, "Incomplete download: $bytesRead / $totalBytes")
                    apkFile.delete()
                    _progress.update { it.copy(state = DownloadState.FAILED) }
                    return@launch
                }

                Log.i(TAG, "Download complete: ${apkFile.length()} bytes")
                _progress.update { it.copy(state = DownloadState.COMPLETED, percent = 1f) }

                // ── Launch installer ────────────────────────────
                withContext(Dispatchers.Main) {
                    installApk(apkFile)
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Download cancelled")
                _progress.update { it.copy(state = DownloadState.IDLE) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _progress.update { it.copy(state = DownloadState.FAILED) }
            } finally {
                conn?.disconnect()
            }
        }
    }

    // ── Private — Install ───────────────────────────────────────────

    private fun installApk(apkFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Log.i(TAG, "Install intent launched")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch install intent", e)
        }
    }

    // ── Private — Cleanup ───────────────────────────────────────────

    /** Deletes all files in the updates cache directory. */
    private fun cleanUpdateCache(updatesDir: File) {
        try {
            updatesDir.listFiles()?.forEach { file ->
                val deleted = file.delete()
                Log.d(TAG, "Cleaned ${file.name}: $deleted")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cache cleanup failed", e)
        }
    }

    // ── Private — Version parsing ───────────────────────────────────

    private fun parseRelease(json: String): UpdateInfo {
        val obj = JSONObject(json)
        val tagName = obj.getString("tag_name")
        val latestVersion = tagName.removePrefix("v")
        val releaseNotes = obj.optString("body", "")

        var apkUrl = ""
        val assets = obj.getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) {
                apkUrl = asset.getString("browser_download_url")
                break
            }
        }

        val remoteCode = versionToInt(latestVersion)
        val currentCode = versionToInt(CURRENT_VERSION)

        return UpdateInfo(
            latestVersion = latestVersion,
            latestVersionCode = remoteCode,
            downloadUrl = apkUrl,
            releaseNotes = releaseNotes,
            isUpdateAvailable = remoteCode > currentCode
        )
    }

    /**
     * Converts a semver string "major.minor.patch" to an integer
     * for numeric comparison: major×10000 + minor×100 + patch.
     */
    private fun versionToInt(version: String): Int {
        val parts = version.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 10_000 + minor * 100 + patch
    }
}