package uzair.lightpods.android.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uzair.lightpods.android.R
import uzair.lightpods.android.bluetooth.CaseLidState
import uzair.lightpods.android.bluetooth.ConnectionState
import uzair.lightpods.android.bluetooth.GestureMapping
import uzair.lightpods.android.bluetooth.MicrophoneLocation
import uzair.lightpods.android.bluetooth.PodBattery
import uzair.lightpods.android.bluetooth.PodDeviceInfo
import uzair.lightpods.android.bluetooth.PodsUiState
import uzair.lightpods.android.bluetooth.SpoofedModel
import uzair.lightpods.android.ui.components.PermissionStatusCard
import uzair.lightpods.android.ui.theme.BatteryFull
import uzair.lightpods.android.ui.theme.BatteryLow
import uzair.lightpods.android.ui.theme.BatteryMedium
import uzair.lightpods.android.ui.theme.BatteryUnknown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: PodsUiState,
    onNavigateSettings: () -> Unit,
    onRequestNotificationPermission: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollBehavior =
        TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(
            scrollBehavior.nestedScrollConnection
        ),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LightPods",
                            style = MaterialTheme
                                .typography.titleLarge
                                .copy(
                                    fontWeight =
                                        FontWeight.Bold
                                )
                        )
                        Text(
                            text = state.connectionState
                                .label(state.deviceInfo.deviceName),
                            style = MaterialTheme
                                .typography.labelMedium,
                            color = MaterialTheme
                                .colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(
                            imageVector =
                                Icons.Rounded.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
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
        AnimatedContent(
            targetState = state.connectionState,
            transitionSpec = {
                (fadeIn(tween(280)) + slideInVertically(
                    spring(
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it / 5 }
                )).togetherWith(fadeOut(tween(180)))
            },
            label = "connectionTransition"
        ) { connectionState ->
            when (connectionState) {
                ConnectionState.CONNECTED ->
                    ConnectedContent(
                        state = state,
                        innerPadding = innerPadding,
                        onRequestNotificationPermission =
                            onRequestNotificationPermission
                    )

                ConnectionState.SCANNING ->
                    EmptyStateContent(
                        innerPadding = innerPadding,
                        imageRes = R.drawable.case_open_buds,
                        title = "Looking for nearby pods",
                        subtitle = "Keep Bluetooth on and open the case near your phone.",
                        showPulse = true,
                        onRequestNotificationPermission =
                            onRequestNotificationPermission
                    )

                ConnectionState.DISCONNECTED ->
                    EmptyStateContent(
                        innerPadding = innerPadding,
                        imageRes = R.drawable.case_closed,
                        title = "No pods connected",
                        subtitle = "Open the case or bring your earbuds close to start scanning.",
                        showPulse = false,
                        onRequestNotificationPermission =
                            onRequestNotificationPermission
                    )
            }
        }
    }
}

private fun ConnectionState.label(deviceName: String): String {
    return when (this) {
        ConnectionState.CONNECTED ->
            if (deviceName.isBlank()) {
                "Connected"
            } else {
                "$deviceName connected"
            }

        ConnectionState.SCANNING -> "Scanning nearby Bluetooth devices"
        ConnectionState.DISCONNECTED -> "Waiting for AirPods-compatible earbuds"
    }
}

@Composable
private fun ConnectedContent(
    state: PodsUiState,
    innerPadding: PaddingValues,
    onRequestNotificationPermission: (() -> Unit)?
) {
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
        item {
            PermissionStatusCard(
                onRequestNotificationPermission =
                    onRequestNotificationPermission
            )
        }
        item { DeviceHeroCard(state) }
        item { BatteryOverviewCard(state.battery) }
        item { LiveStateCard(state) }
        item { DeviceIdentityCard(state.deviceInfo, state.rssi) }
        item { GestureCard(state.gestures) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DeviceHeroCard(state: PodsUiState) {
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme
                                .primaryContainer
                                .copy(alpha = 0.92f),
                            MaterialTheme.colorScheme
                                .surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = state.deviceInfo
                                .deviceName
                                .ifBlank { "LightPods" },
                            style = MaterialTheme
                                .typography.headlineSmall
                                .copy(
                                    fontWeight =
                                        FontWeight.Bold
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.deviceInfo
                                .spoofedModel.label,
                            style = MaterialTheme
                                .typography.bodyMedium,
                            color = MaterialTheme
                                .colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    StatusPill(
                        label = "Live",
                        color = BatteryFull
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.Center
                    ) {
                        PodImage(
                            res = R.drawable.pod_left,
                            contentDescription = "Left pod",
                            modifier = Modifier.size(92.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        PodImage(
                            res = R.drawable.case_closed,
                            contentDescription = "Case",
                            modifier = Modifier.size(118.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        PodImage(
                            res = R.drawable.pod_right,
                            contentDescription = "Right pod",
                            modifier = Modifier.size(92.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    QuickMetric(
                        label = "Case",
                        value = when (state.caseLidState) {
                            CaseLidState.OPEN -> "Open"
                            CaseLidState.CLOSED -> "Closed"
                            CaseLidState.NOT_IN_CASE -> "Away"
                            CaseLidState.UNKNOWN -> "Unknown"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickMetric(
                        label = "Mic",
                        value = when (state.micLocation) {
                            MicrophoneLocation.LEFT -> "Left"
                            MicrophoneLocation.RIGHT -> "Right"
                            MicrophoneLocation.UNKNOWN -> "Unknown"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickMetric(
                        label = "Signal",
                        value = if (state.rssi == 0) {
                            "Unknown"
                        } else {
                            "${state.rssi} dBm"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PodImage(
    res: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(res),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun StatusPill(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.18f),
        contentColor = color,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme
                    .typography.labelLarge
                    .copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun QuickMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme
            .surface.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme
                    .typography.labelLarge
                    .copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BatteryOverviewCard(battery: PodBattery) {
    SectionCard(
        title = "Battery",
        subtitle = "Live levels decoded from BLE proximity packets"
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 6.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BatteryMeter(
                label = "Left pod",
                percent = battery.leftPercent,
                charging = battery.isLeftCharging,
                isDead = battery.isLeftDead
            )
            BatteryMeter(
                label = "Right pod",
                percent = battery.rightPercent,
                charging = battery.isRightCharging,
                isDead = battery.isRightDead
            )
            BatteryMeter(
                label = "Case",
                percent = battery.casePercent,
                charging = battery.isCaseCharging,
                isDead = battery.isCaseDead
            )
        }
    }
}

@Composable
private fun BatteryMeter(
    label: String,
    percent: Int,
    charging: Boolean,
    isDead: Boolean = false
) {
    val isAvailable = percent in 0..100
    val color = if (isDead) BatteryLow else batteryColor(percent)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = label,
                    style = MaterialTheme
                        .typography.bodyMedium
                        .copy(fontWeight = FontWeight.Medium)
                )
                if (isDead) {
                    Text(
                        text = "Dead",
                        style = MaterialTheme.typography.labelSmall,
                        color = BatteryLow
                    )
                } else if (charging) {
                    Text(
                        text = "Charging",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = when {
                    isDead -> "Dead"
                    isAvailable -> "$percent%"
                    else -> "Unknown"
                },
                style = MaterialTheme
                    .typography.labelLarge
                    .copy(fontWeight = FontWeight.Bold),
                color = when {
                    isDead -> BatteryLow
                    isAvailable ->
                        MaterialTheme.colorScheme.onSurface
                    else ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        LinearProgressIndicator(
            progress = {
                if (isDead) 0f
                else if (isAvailable) percent / 100f
                else 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme
                .surfaceVariant.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun LiveStateCard(state: PodsUiState) {
    SectionCard(
        title = "Live state",
        subtitle = "Ear, microphone, case, and scan status"
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 6.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusRow(
                label = "Left ear",
                value = when {
                    state.battery.isLeftDead -> "Dead"
                    state.isLeftInEar -> "In ear"
                    state.isLeftInCase -> "In case"
                    else -> "Out"
                },
                active = state.isLeftInEar
            )
            StatusRow(
                label = "Right ear",
                value = when {
                    state.battery.isRightDead -> "Dead"
                    state.isRightInEar -> "In ear"
                    state.isRightInCase -> "In case"
                    else -> "Out"
                },
                active = state.isRightInEar
            )
            StatusRow(
                label = "Microphone",
                value = when (state.micLocation) {
                    MicrophoneLocation.LEFT -> "Left pod"
                    MicrophoneLocation.RIGHT -> "Right pod"
                    MicrophoneLocation.UNKNOWN -> "Unknown"
                },
                active = state.micLocation !=
                    MicrophoneLocation.UNKNOWN
            )
            StatusRow(
                label = "Case lid",
                value = when (state.caseLidState) {
                    CaseLidState.OPEN -> "Open"
                    CaseLidState.CLOSED -> "Closed"
                    CaseLidState.NOT_IN_CASE -> "Pods outside"
                    CaseLidState.UNKNOWN -> "Unknown"
                },
                active = state.isCaseLidOpen
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    active: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme
            .surfaceVariant.copy(alpha = 0.46f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) {
                                BatteryFull
                            } else {
                                MaterialTheme.colorScheme
                                    .outline.copy(alpha = 0.55f)
                            }
                        )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme
                    .typography.bodyMedium
                    .copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeviceIdentityCard(
    info: PodDeviceInfo,
    rssi: Int
) {
    SectionCard(
        title = "Device identity",
        subtitle = "Model spoofing and packet metadata"
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            InfoRow("Model", info.spoofedModel.label)
            InfoDivider()
            InfoRow("Device code", info.rawDeviceCodeHex)
            InfoDivider()
            InfoRow("Name", info.deviceName.ifBlank { "Unknown" })
            InfoDivider()
            InfoRow("MAC", info.macAddress.ifBlank { "Unknown" })
            InfoDivider()
            InfoRow("Packet length", "${info.messageLength}")
            InfoDivider()
            InfoRow(
                "Authenticity",
                if (info.isFake) {
                    "Clone signature"
                } else if (info.spoofedModel ==
                    SpoofedModel.UNKNOWN
                ) {
                    "Unknown"
                } else {
                    "Standard signature"
                }
            )
            if (rssi != 0) {
                InfoDivider()
                InfoRow("RSSI", "$rssi dBm")
            }
        }
    }
}

@Composable
private fun GestureCard(
    gestures: List<GestureMapping>
) {
    SectionCard(
        title = "Touch controls",
        subtitle = "Detected clone gesture mapping"
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            gestures.forEachIndexed { index, mapping ->
                InfoRow(
                    label = mapping.gesture,
                    value = mapping.action.displayName
                )
                if (index < gestures.lastIndex) {
                    InfoDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
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
                ),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme
                        .typography.titleMedium
                        .copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.9f)
        )
        Text(
            text = value,
            style = MaterialTheme
                .typography.bodyMedium
                .copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1.1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme
            .outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
private fun EmptyStateContent(
    innerPadding: PaddingValues,
    imageRes: Int,
    title: String,
    subtitle: String,
    showPulse: Boolean,
    onRequestNotificationPermission: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.92f),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = title,
                    modifier = Modifier
                        .size(180.dp)
                        .alpha(if (showPulse) 1f else 0.72f),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    text = title,
                    style = MaterialTheme
                        .typography.headlineSmall
                        .copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant
                )
                if (showPulse) {
                    Spacer(modifier = Modifier.height(24.dp))
                    PulsingDots()
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        PermissionStatusCard(
            onRequestNotificationPermission =
                onRequestNotificationPermission
        )
    }
}

@Composable
private fun PulsingDots() {
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "pulsingDots"
        )
    Row(
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { index ->
            val alpha by
                infiniteTransition.animateFloat(
                    initialValue = 0.28f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 620,
                            delayMillis = index * 180,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$index"
                )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = alpha)
                    )
            )
        }
    }
}

@Composable
private fun batteryColor(percent: Int): Color {
    return when {
        percent !in 0..100 -> BatteryUnknown
        percent > 50 -> BatteryFull
        percent > 20 -> BatteryMedium
        else -> BatteryLow
    }
}

