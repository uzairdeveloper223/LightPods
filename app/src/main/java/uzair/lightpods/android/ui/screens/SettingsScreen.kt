package uzair.lightpods.android.ui.screens

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import uzair.lightpods.android.settings.ThemeMode
import uzair.lightpods.android.ui.theme.BatteryFull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateAbout: () -> Unit,
    onCheckUpdate: () -> Unit = {},
    onRequestNotificationPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme
                            .typography.titleLarge
                            .copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector =
                                Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme.background,
                    scrolledContainerColor =
                        MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SettingsHero() }
            item { ThemeSection(currentTheme, onThemeChange) }
            item {
                PermissionsSection(
                    onRequestNotificationPermission =
                        onRequestNotificationPermission
                )
            }
            item {
                SettingsGroup(
                    title = "App",
                    subtitle = "Updates, source, and privacy"
                ) {
                    SettingsActionRow(
                        icon = Icons.Rounded.SystemUpdate,
                        label = "Check for updates",
                        subtitle = "Download the latest release",
                        onClick = onCheckUpdate
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Rounded.Info,
                        label = "About LightPods",
                        subtitle = "Version, license, and developer info",
                        onClick = onNavigateAbout
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Rounded.PrivacyTip,
                        label = "Privacy policy",
                        subtitle = "No analytics, accounts, or cloud sync",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        "https://lightpods.ct.ws/privacy/index.html"
                                    )
                                )
                            )
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsHero() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme
                                .primaryContainer,
                            MaterialTheme.colorScheme
                                .tertiaryContainer
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "LightPods control center",
                    style = MaterialTheme
                        .typography.titleLarge
                        .copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tune appearance, permissions, updates, and privacy from one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeSection(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    SettingsGroup(
        title = "Appearance",
        subtitle = "Match Android dynamic color or lock a mode"
    ) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            ThemeChoiceRow(
                mode = mode,
                selected = currentTheme == mode,
                onClick = { onThemeChange(mode) }
            )
            if (index < ThemeMode.entries.lastIndex) {
                SettingsDivider()
            }
        }
    }
}

@Composable
private fun ThemeChoiceRow(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ThemeSwatch(mode)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = mode.label(),
                style = MaterialTheme
                    .typography.bodyLarge
                    .copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = mode.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

@Composable
private fun ThemeSwatch(mode: ThemeMode) {
    val colors = when (mode) {
        ThemeMode.SYSTEM -> listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.surfaceVariant
        )

        ThemeMode.LIGHT -> listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surface
        )

        ThemeMode.DARK -> listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onSurface
        )
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(colors)
            )
    )
}

private fun ThemeMode.label(): String {
    return when (this) {
        ThemeMode.SYSTEM -> "System default"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
}

private fun ThemeMode.description(): String {
    return when (this) {
        ThemeMode.SYSTEM ->
            "Follow Android and dynamic color"
        ThemeMode.LIGHT ->
            "Bright surfaces for daytime use"
        ThemeMode.DARK ->
            "Dim surfaces for low light"
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 16.dp
                )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme
                        .typography.titleMedium
                        .copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme
                .secondaryContainer
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(11.dp)
                    .size(22.dp),
                tint = MaterialTheme.colorScheme
                    .onSecondaryContainer
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme
                    .typography.bodyLarge
                    .copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Open",
            style = MaterialTheme
                .typography.labelLarge
                .copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PermissionsSection(
    onRequestNotificationPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Refresh permission state when user returns
    // from system settings.
    var refreshKey by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshKey++
    }

    // ── Check all permissions ──
    val btManager = remember(refreshKey) {
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as? BluetoothManager
    }
    val btEnabled = remember(refreshKey) {
        btManager?.adapter?.isEnabled == true
    }
    val hasBtPerms = remember(refreshKey) {
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            hasPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) && hasPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
        } else true
    }
    val hasLocation = remember(refreshKey) {
        hasPermission(
            context,
            android.Manifest.permission
                .ACCESS_FINE_LOCATION
        )
    }
    val hasBgLocation = remember(refreshKey) {
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            hasPermission(
                context,
                android.Manifest.permission
                    .ACCESS_BACKGROUND_LOCATION
            )
        } else true
    }
    val hasOverlay = remember(refreshKey) {
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {
            AndroidSettings.canDrawOverlays(context)
        } else true
    }
    val hasBatteryExempt = remember(refreshKey) {
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {
            val pm = context.getSystemService(
                Context.POWER_SERVICE
            ) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(
                context.packageName
            ) ?: true
        } else true
    }
    val hasNotifications = remember(refreshKey) {
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            hasPermission(
                context,
                android.Manifest.permission
                    .POST_NOTIFICATIONS
            )
        } else true
    }

    val allGranted = btEnabled && hasBtPerms &&
        hasLocation && hasBgLocation &&
        hasOverlay && hasBatteryExempt &&
        hasNotifications

    SettingsGroup(
        title = "Permissions",
        subtitle = if (allGranted)
            "All permissions granted — everything is ready"
        else
            "Some permissions need attention for reliable scanning"
    ) {
        // Bluetooth
        SettingsPermissionRow(
            icon = Icons.Rounded.Settings,
            label = "Bluetooth",
            subtitle = if (btEnabled) "Enabled"
            else "Turn on Bluetooth to scan",
            isGranted = btEnabled,
            onClick = {
                context.startActivity(
                    Intent(
                        AndroidSettings
                            .ACTION_BLUETOOTH_SETTINGS
                    )
                )
            }
        )

        // BT Connect/Scan (Android 12+)
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            SettingsDivider()
            SettingsPermissionRow(
                icon = Icons.Rounded.Settings,
                label = "Bluetooth permission",
                subtitle = if (hasBtPerms)
                    "Connect & scan access granted"
                else "Required for BLE scanning",
                isGranted = hasBtPerms,
                onClick = { openAppSettings(context) }
            )
        }

        // Location
        SettingsDivider()
        SettingsPermissionRow(
            icon = Icons.Rounded.Settings,
            label = "Location",
            subtitle = if (hasLocation)
                "Fine location access granted"
            else "Required for BLE discovery",
            isGranted = hasLocation,
            onClick = { openAppSettings(context) }
        )

        // Background Location (Android 10+)
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            SettingsDivider()
            SettingsPermissionRow(
                icon = Icons.Rounded.Settings,
                label = "Background location",
                subtitle = if (hasBgLocation)
                    "Background scanning allowed"
                else "Needed for always-on monitoring",
                isGranted = hasBgLocation,
                onClick = { openAppSettings(context) }
            )
        }

        // Overlay
        SettingsDivider()
        SettingsPermissionRow(
            icon = Icons.Rounded.Settings,
            label = "Overlay permission",
            subtitle = if (hasOverlay)
                "Connection popup can appear"
            else "Required to show popup",
            isGranted = hasOverlay,
            onClick = {
                if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M
                ) {
                    context.startActivity(
                        Intent(
                            AndroidSettings
                                .ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse(
                                "package:" +
                                    context.packageName
                            )
                        )
                    )
                }
            }
        )

        // Battery optimization
        SettingsDivider()
        SettingsPermissionRow(
            icon = Icons.Rounded
                .BatteryChargingFull,
            label = "Battery optimization",
            subtitle = if (hasBatteryExempt)
                "Exempt from battery restrictions"
            else "Disable for reliable scanning",
            isGranted = hasBatteryExempt,
            onClick = {
                if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M
                ) {
                    context.startActivity(
                        Intent(
                            AndroidSettings
                                .ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                        )
                    )
                }
            }
        )

        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            SettingsDivider()
            SettingsPermissionRow(
                icon = Icons.Rounded.Settings,
                label = "Notifications",
                subtitle = if (hasNotifications)
                    "Status notifications enabled"
                else "Needed for battery alerts",
                isGranted = hasNotifications,
                onClick = {
                    onRequestNotificationPermission
                        ?.invoke()
                        ?: openAppSettings(context)
                }
            )
        }
    }
}

@Composable
private fun SettingsPermissionRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isGranted) {
                BatteryFull.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme
                    .secondaryContainer
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(11.dp)
                    .size(22.dp),
                tint = if (isGranted) {
                    BatteryFull
                } else {
                    MaterialTheme.colorScheme
                        .onSecondaryContainer
                }
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme
                    .typography.bodyLarge
                    .copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant
            )
        }
        if (isGranted) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector =
                        Icons.Rounded.CheckCircle,
                    contentDescription = "Granted",
                    modifier = Modifier.size(16.dp),
                    tint = BatteryFull
                )
                Text(
                    text = "Granted",
                    style = MaterialTheme
                        .typography.labelLarge
                        .copy(
                            fontWeight = FontWeight.Bold
                        ),
                    color = BatteryFull
                )
            }
        } else {
            Text(
                text = "Open",
                style = MaterialTheme
                    .typography.labelLarge
                    .copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme
            .outlineVariant.copy(alpha = 0.45f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

private fun hasPermission(
    context: Context,
    permission: String
): Boolean {
    return ContextCompat.checkSelfPermission(
        context, permission
    ) == PackageManager.PERMISSION_GRANTED
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            AndroidSettings
                .ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse(
                "package:" + context.packageName
            )
        )
    )
}
