package uzair.lightpods.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uzair.lightpods.android.service.PodsMonitorService
import uzair.lightpods.android.settings.ThemeMode
import uzair.lightpods.android.ui.components.ConnectionSheet
import uzair.lightpods.android.ui.screens.AboutScreen
import uzair.lightpods.android.ui.screens.HomeScreen
import uzair.lightpods.android.ui.screens.SettingsScreen
import uzair.lightpods.android.ui.theme.LightPodsTheme
import uzair.lightpods.android.ui.viewmodel.PodsViewModel

class MainActivity : ComponentActivity() {

    private val bluetoothPermissions: Array<String>
        get() = if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
        ) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

    private var onPermissionsGranted: (() -> Unit)? =
        null
    private var onBgLocationGranted: (() -> Unit)? =
        null

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                onPermissionsGranted?.invoke()
                requestBackgroundLocation()
            }
        }

    private val bgLocationLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) { granted ->
            if (granted) {
                onBgLocationGranted?.invoke()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: PodsViewModel = viewModel()
            val uiState by viewModel.uiState
                .collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode
                .collectAsStateWithLifecycle()
            val showUpdate by viewModel.showUpdateDialog
                .collectAsStateWithLifecycle()
            val updateInfo by viewModel.updateInfo
                .collectAsStateWithLifecycle()

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            LightPodsTheme(darkTheme = isDark) {
                var currentScreen by remember {
                    mutableStateOf("home")
                }

                when (currentScreen) {
                    "home" -> HomeScreen(
                        state = uiState,
                        onNavigateSettings = {
                            currentScreen = "settings"
                        },
                        onRequestNotificationPermission = {
                            requestNotificationPermission()
                        }
                    )
                    "settings" -> SettingsScreen(
                        currentTheme = themeMode,
                        onThemeChange = {
                            viewModel.setThemeMode(it)
                        },
                        onNavigateBack = {
                            currentScreen = "home"
                        },
                        onNavigateAbout = {
                            currentScreen = "about"
                        },
                        onCheckUpdate = {
                            viewModel.checkForUpdates(
                                showResult = true
                            )
                        },
                        onRequestNotificationPermission = {
                            requestNotificationPermission()
                        }
                    )
                    "about" -> AboutScreen(
                        onNavigateBack = {
                            currentScreen = "settings"
                        }
                    )
                }

                if (showUpdate && updateInfo != null) {
                    val dlProgress by viewModel
                        .downloadProgress
                        .collectAsStateWithLifecycle()

                    uzair.lightpods.android.updater
                        .UpdateDialog(
                            updateInfo = updateInfo!!,
                            downloadProgress =
                                dlProgress,
                            onUpdate = {
                                viewModel.downloadUpdate()
                            },
                            onDismiss = {
                                viewModel
                                    .dismissUpdateDialog()
                            }
                        )
                }

                if (uiState.showConnectionSheet) {
                    ConnectionSheet(
                        deviceName = uiState.deviceInfo
                            .deviceName
                            .ifBlank { "LightPods" },
                        battery = uiState.battery,
                        micLocation = uiState.micLocation,
                        onDismiss = {
                            viewModel.dismissConnectionSheet()
                        }
                    )
                }
            }

            LaunchedEffect(Unit) {
                requestAllPermissions {
                    viewModel.initializeBluetooth()
                    PodsMonitorService.start(
                        this@MainActivity
                    )
                }
                viewModel.checkForUpdates()
            }
        }
    }

    private fun requestAllPermissions(
        onGranted: () -> Unit
    ) {
        val hasAll = bluetoothPermissions.all { perm ->
            ContextCompat.checkSelfPermission(
                this, perm
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (hasAll) {
            onGranted()
            requestBackgroundLocation()
            return
        }
        onPermissionsGranted = onGranted
        permissionLauncher.launch(bluetoothPermissions)
    }

    private fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT <
            Build.VERSION_CODES.Q
        ) return

        val hasBg = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission
                .ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasBg) {
            bgLocationLauncher.launch(
                Manifest.permission
                    .ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU
        ) return

        val hasNotifications =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasNotifications) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }
}
