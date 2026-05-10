package uzair.lightpods.android.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val isUpdateAvailable: Boolean
)

data class DownloadProgress(
    val state: DownloadState = DownloadState.IDLE,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = -1,
    val percent: Float = 0f
)

enum class DownloadState {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

class AppUpdater(private val context: Context) {

    companion object {
        private const val GITHUB_API =
            "https://api.github.com/repos/" +
                "uzairdeveloper223/lightpods/" +
                "releases/latest"
        private const val CURRENT_VERSION = "1.0.0"
        private const val CURRENT_VERSION_CODE = 1
    }

    private val _downloadProgress =
        MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> =
        _downloadProgress.asStateFlow()

    private var activeDownloadId: Long = -1
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    suspend fun checkForUpdate(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API)
                val conn =
                    url.openConnection() as
                        HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty(
                    "Accept",
                    "application/vnd.github.v3+json"
                )
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode != 200) {
                    return@withContext null
                }

                val body = conn.inputStream
                    .bufferedReader()
                    .readText()
                conn.disconnect()

                val json = JSONObject(body)
                val tagName =
                    json.getString("tag_name")
                val releaseNotes =
                    json.optString("body", "")
                val latestVersion =
                    tagName.removePrefix("v")

                var apkUrl = ""
                val assets =
                    json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val asset =
                        assets.getJSONObject(i)
                    val name =
                        asset.getString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.getString(
                            "browser_download_url"
                        )
                        break
                    }
                }

                val remoteCode =
                    parseVersionCode(latestVersion)
                val currentCode =
                    parseVersionCode(
                        CURRENT_VERSION
                    )
                val isNewer =
                    remoteCode > currentCode

                UpdateInfo(
                    latestVersion = latestVersion,
                    latestVersionCode = remoteCode,
                    downloadUrl = apkUrl,
                    releaseNotes = releaseNotes,
                    isUpdateAvailable = isNewer
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun downloadAndInstall(
        downloadUrl: String,
        version: String
    ) {
        val fileName =
            "LightPods-v${version}.apk"

        _downloadProgress.value = DownloadProgress(
            state = DownloadState.DOWNLOADING
        )

        val request = DownloadManager.Request(
            Uri.parse(downloadUrl)
        ).apply {
            setTitle("LightPods v$version")
            setDescription("Downloading update…")
            setNotificationVisibility(
                DownloadManager.Request
                    .VISIBILITY_VISIBLE
            )
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
            setMimeType(
                "application/vnd" +
                    ".android.package-archive"
            )
        }

        val dm = context.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager
        activeDownloadId = dm.enqueue(request)

        // Start polling progress
        startProgressTracking(dm)

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent
                ) {
                    val id = intent.getLongExtra(
                        DownloadManager
                            .EXTRA_DOWNLOAD_ID,
                        -1
                    )
                    if (id != activeDownloadId) return
                    ctx.unregisterReceiver(this)
                    stopProgressTracking()

                    _downloadProgress.value =
                        DownloadProgress(
                            state =
                                DownloadState.COMPLETED,
                            percent = 1f
                        )

                    installApk(fileName)
                }
            }

        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            context.registerReceiver(
                receiver,
                IntentFilter(
                    DownloadManager
                        .ACTION_DOWNLOAD_COMPLETE
                ),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(
                    DownloadManager
                        .ACTION_DOWNLOAD_COMPLETE
                )
            )
        }
    }

    private fun startProgressTracking(
        dm: DownloadManager
    ) {
        progressRunnable = object : Runnable {
            override fun run() {
                val query = DownloadManager.Query()
                    .setFilterById(activeDownloadId)
                val cursor = dm.query(query)

                if (cursor != null &&
                    cursor.moveToFirst()
                ) {
                    val statusIdx = cursor
                        .getColumnIndex(
                            DownloadManager
                                .COLUMN_STATUS
                        )
                    val downloadedIdx = cursor
                        .getColumnIndex(
                            DownloadManager
                                .COLUMN_BYTES_DOWNLOADED_SO_FAR
                        )
                    val totalIdx = cursor
                        .getColumnIndex(
                            DownloadManager
                                .COLUMN_TOTAL_SIZE_BYTES
                        )

                    if (statusIdx >= 0 &&
                        downloadedIdx >= 0 &&
                        totalIdx >= 0
                    ) {
                        val status =
                            cursor.getInt(statusIdx)
                        val downloaded =
                            cursor.getLong(
                                downloadedIdx
                            )
                        val total =
                            cursor.getLong(totalIdx)

                        val pct = if (total > 0)
                            downloaded.toFloat() /
                                total.toFloat()
                        else 0f

                        when (status) {
                            DownloadManager
                                .STATUS_RUNNING -> {
                                _downloadProgress
                                    .value =
                                    DownloadProgress(
                                        state =
                                            DownloadState
                                                .DOWNLOADING,
                                        bytesDownloaded =
                                            downloaded,
                                        totalBytes =
                                            total,
                                        percent = pct
                                    )
                            }
                            DownloadManager
                                .STATUS_FAILED -> {
                                _downloadProgress
                                    .value =
                                    DownloadProgress(
                                        state =
                                            DownloadState
                                                .FAILED
                                    )
                                stopProgressTracking()
                                cursor.close()
                                return
                            }
                        }
                    }
                    cursor.close()
                }
                handler.postDelayed(
                    this, 300
                )
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressTracking() {
        progressRunnable?.let {
            handler.removeCallbacks(it)
        }
        progressRunnable = null
    }

    fun resetProgress() {
        _downloadProgress.value =
            DownloadProgress()
    }

    private fun installApk(fileName: String) {
        val file = File(
            Environment
                .getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ),
            fileName
        )
        if (!file.exists()) return

        val uri = if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}" +
                    ".fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(
            Intent.ACTION_VIEW
        ).apply {
            setDataAndType(
                uri,
                "application/vnd" +
                    ".android.package-archive"
            )
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    private fun parseVersionCode(
        version: String
    ): Int {
        val parts = version.split(".")
        return try {
            val major = parts.getOrNull(0)
                ?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)
                ?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)
                ?.toIntOrNull() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (_: Exception) { 0 }
    }
}
