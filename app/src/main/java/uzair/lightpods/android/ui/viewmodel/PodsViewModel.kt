package uzair.lightpods.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uzair.lightpods.android.bluetooth.BluetoothPodsManager
import uzair.lightpods.android.bluetooth.PodsUiState
import uzair.lightpods.android.settings.AppSettings
import uzair.lightpods.android.settings.ThemeMode
import uzair.lightpods.android.updater.AppUpdater
import uzair.lightpods.android.updater.DownloadProgress
import uzair.lightpods.android.updater.UpdateInfo

class PodsViewModel(application: Application) :
    AndroidViewModel(application) {

    private val podsManager = BluetoothPodsManager
        .getInstance(application.applicationContext)

    val appSettings = AppSettings(
        application.applicationContext
    )

    val appUpdater = AppUpdater(
        application.applicationContext
    )

    val uiState = podsManager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PodsUiState()
    )

    val themeMode = appSettings.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    private val _updateInfo =
        MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> =
        _updateInfo.asStateFlow()

    private val _showUpdateDialog =
        MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> =
        _showUpdateDialog.asStateFlow()

    val downloadProgress:
        StateFlow<DownloadProgress> =
        appUpdater.progress

    fun initializeBluetooth() {
        podsManager.initialize()
    }

    fun releaseBluetooth() {
        podsManager.release()
    }

    fun dismissConnectionSheet() {
        podsManager.dismissConnectionSheet()
    }

    fun connectDemo() {
        podsManager.connectForDemo()
    }

    fun setThemeMode(mode: ThemeMode) {
        appSettings.setThemeMode(mode)
    }

    fun checkForUpdates(showResult: Boolean = false) {
        viewModelScope.launch {
            val info = appUpdater.checkForUpdate()
            _updateInfo.value = info ?: if (showResult) {
                UpdateInfo(
                    latestVersion = AppUpdater.CURRENT_VERSION,
                    latestVersionCode = 0,
                    downloadUrl = "",
                    releaseNotes = "Could not reach the update server. Check your connection and try again.",
                    isUpdateAvailable = false
                )
            } else {
                null
            }

            if (info?.isUpdateAvailable == true ||
                showResult
            ) {
                _showUpdateDialog.value = true
            }
        }
    }

    fun downloadUpdate() {
        val info = _updateInfo.value ?: return
        if (info.downloadUrl.isNotBlank()) {
            appUpdater.downloadAndInstall(
                info.downloadUrl,
                info.latestVersion
            )
        }
        // Keep dialog open to show progress
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
        appUpdater.resetProgress()
    }

    override fun onCleared() {
        super.onCleared()
        appUpdater.release()
        // Don't release the singleton manager —
        // the foreground service still needs it
    }
}
