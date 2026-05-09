package uzair.lightpods.android.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uzair.lightpods.android.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateAbout: () -> Unit,
    onCheckUpdate: () -> Unit = {}
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(
            scrollBehavior.nestedScrollConnection
        ),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme
                            .typography.headlineMedium
                            .copy(
                                fontWeight =
                                    FontWeight.Bold
                            )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            painter = painterResource(
                                android.R.drawable
                                    .ic_menu_revert
                            ),
                            contentDescription = "Back",
                            modifier =
                                Modifier.size(24.dp)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults
                    .largeTopAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme
                                .background,
                        scrolledContainerColor =
                            MaterialTheme.colorScheme
                                .surface
                    )
            )
        },
        containerColor =
            MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            item { ThemeSection(currentTheme, onThemeChange) }

            item {
                SettingsCard(title = "Permissions") {
                    SettingsRow(
                        label = "Overlay Permission",
                        subtitle =
                            "Display popup over apps"
                    ) {
                        if (Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.M
                        ) {
                            context.startActivity(
                                Intent(
                                    Settings
                                        .ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse(
                                        "package:" +
                                            context.packageName
                                    )
                                )
                            )
                        }
                    }
                    SettingsDivider()
                    SettingsRow(
                        label = "Battery Optimization",
                        subtitle =
                            "Disable to keep monitoring"
                    ) {
                        if (Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.M
                        ) {
                            context.startActivity(
                                Intent(
                                    Settings
                                        .ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                )
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard(title = "Info") {
                    SettingsRow(
                        label = "Check for Updates",
                        subtitle =
                            "Download latest version"
                    ) { onCheckUpdate() }
                    SettingsDivider()
                    SettingsRow(
                        label = "About",
                        subtitle =
                            "App info & developer"
                    ) { onNavigateAbout() }
                    SettingsDivider()
                    SettingsRow(
                        label = "Privacy Policy",
                        subtitle =
                            "How we handle your data"
                    ) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "https://lightpods-privacy.uzair.ct.ws"
                                )
                            )
                        )
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(24.dp)
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
    SettingsCard(title = "Appearance") {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeChange(mode) }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (mode) {
                        ThemeMode.SYSTEM ->
                            "System Default"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    style = MaterialTheme
                        .typography.bodyLarge,
                    color = MaterialTheme
                        .colorScheme.onSurface
                )
                RadioButton(
                    selected =
                        currentTheme == mode,
                    onClick = { onThemeChange(mode) }
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme
                    .typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                color = MaterialTheme
                    .colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 4.dp
                )
            )
            content()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme
                .typography.bodyLarge,
            color = MaterialTheme
                .colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme
                .typography.bodySmall,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme
            .outlineVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(
            horizontal = 16.dp
        )
    )
}
