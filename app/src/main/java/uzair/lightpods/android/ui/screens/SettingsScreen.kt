package uzair.lightpods.android.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import uzair.lightpods.android.settings.ThemeMode
import uzair.lightpods.android.ui.components.PermissionStatusCard

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
            item {
                PermissionStatusCard(
                    onRequestNotificationPermission =
                        onRequestNotificationPermission,
                    showGrantedStatuses = true
                )
            }
            item { ThemeSection(currentTheme, onThemeChange) }
            item {
                SettingsGroup(
                    title = "Permissions",
                    subtitle = "Keep the monitor alive and let popups appear"
                ) {
                    SettingsActionRow(
                        icon = Icons.Rounded.Settings,
                        label = "Overlay permission",
                        subtitle = "Show connection popup over other apps",
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
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Rounded
                            .BatteryChargingFull,
                        label = "Battery optimization",
                        subtitle = "Disable restrictions for reliable scanning",
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
                }
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
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme
            .outlineVariant.copy(alpha = 0.45f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
