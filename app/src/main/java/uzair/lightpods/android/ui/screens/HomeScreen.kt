package uzair.lightpods.android.ui.screens

import uzair.lightpods.android.ui.components.PermissionStatusCard

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uzair.lightpods.android.R
import uzair.lightpods.android.bluetooth.ConnectionState
import uzair.lightpods.android.bluetooth.GestureMapping
import uzair.lightpods.android.bluetooth.MicrophoneLocation
import uzair.lightpods.android.bluetooth.PodBattery
import uzair.lightpods.android.bluetooth.PodDeviceInfo
import uzair.lightpods.android.bluetooth.PodsUiState
import uzair.lightpods.android.ui.components.BatteryRow
import uzair.lightpods.android.ui.theme.BatteryFull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: PodsUiState,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(
            scrollBehavior.nestedScrollConnection
        ),
        topBar = {
            PodsTopBar(
                deviceName = state.deviceInfo.deviceName,
                isConnected = state.connectionState ==
                    ConnectionState.CONNECTED,
                scrollBehavior = scrollBehavior,
                onSettingsTap = onNavigateSettings
            )
        },
        containerColor =
            MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.connectionState,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(
                    spring(
                        stiffness = Spring.StiffnessLow
                    ),
                    initialOffsetY = { it / 4 }
                )).togetherWith(fadeOut(tween(200)))
            },
            label = "connectionTransition"
        ) { connectionState ->
            when (connectionState) {
                ConnectionState.CONNECTED ->
                    ConnectedContent(
                        state = state,
                        innerPadding = innerPadding
                    )
                ConnectionState.SCANNING ->
                    ScanningContent(
                        innerPadding = innerPadding
                    )
                ConnectionState.DISCONNECTED ->
                    DisconnectedContent(
                        innerPadding = innerPadding
                    )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodsTopBar(
    deviceName: String,
    isConnected: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onSettingsTap: () -> Unit
) {
    LargeTopAppBar(
        title = {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text = deviceName,
                    style = MaterialTheme
                        .typography.headlineMedium
                        .copy(
                            fontWeight = FontWeight.Bold
                        )
                )
                if (isConnected) {
                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BatteryFull)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSettingsTap) {
                Icon(
                    painter = painterResource(
                        android.R.drawable
                            .ic_menu_preferences
                    ),
                    contentDescription = "Settings",
                    tint = MaterialTheme
                        .colorScheme.onSurface
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
}

@Composable
private fun ConnectedContent(
    state: PodsUiState,
    innerPadding: PaddingValues
) {
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
        item { PermissionStatusCard() }
        item { HeroCard(state) }
        item { BatteryCard(state.battery) }
        item { StatusCard(state) }
        item { DeviceInfoCard(state.deviceInfo) }
        item { GestureCard(state.gestures) }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroCard(state: PodsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme
                                .surfaceVariant
                                .copy(alpha = 0.3f),
                            MaterialTheme.colorScheme
                                .surface
                                .copy(alpha = 0.6f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.Center
            ) {
                Image(
                    painter = painterResource(
                        R.drawable.pod_left
                    ),
                    contentDescription = "Left Pod",
                    modifier = Modifier.size(90.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                Image(
                    painter = painterResource(
                        R.drawable.pod_right
                    ),
                    contentDescription = "Right Pod",
                    modifier = Modifier.size(90.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(
                    modifier = Modifier.width(16.dp)
                )
                Image(
                    painter = painterResource(
                        R.drawable.case_closed
                    ),
                    contentDescription = "Case",
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            shadowElevation = 8f
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun BatteryCard(battery: PodBattery) {
    SectionCard(title = "Battery") {
        BatteryRow(
            leftPercent = battery.leftPercent,
            rightPercent = battery.rightPercent,
            casePercent = battery.casePercent,
            modifier = Modifier.padding(
                vertical = 8.dp
            )
        )
    }
}

@Composable
private fun StatusCard(state: PodsUiState) {
    SectionCard(title = "Status") {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        ) {
            InfoRow(
                "Microphone",
                when (state.micLocation) {
                    MicrophoneLocation.LEFT ->
                        "\uD83C\uDFA4 Left Pod"
                    MicrophoneLocation.RIGHT ->
                        "\uD83C\uDFA4 Right Pod"
                    MicrophoneLocation.UNKNOWN ->
                        "Unknown"
                }
            )
            InfoDivider()
            InfoRow(
                "Left Ear",
                if (state.isLeftInEar)
                    "\uD83D\uDC42 In Ear" else "Out"
            )
            InfoDivider()
            InfoRow(
                "Right Ear",
                if (state.isRightInEar)
                    "\uD83D\uDC42 In Ear" else "Out"
            )
            InfoDivider()
            InfoRow(
                "Case Lid",
                if (state.isCaseLidOpen)
                    "Open" else "Closed"
            )
            if (state.rssi != 0) {
                InfoDivider()
                InfoRow(
                    "Signal",
                    "${state.rssi} dBm"
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(info: PodDeviceInfo) {
    SectionCard(title = "Device Info") {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        ) {
            InfoRow(
                "Spoofed Model",
                info.spoofedModel.label
            )
            InfoDivider()
            InfoRow(
                "Device Code",
                info.rawDeviceCodeHex
            )
            InfoDivider()
            InfoRow(
                "Name",
                info.deviceName.ifEmpty { "—" }
            )
            InfoDivider()
            InfoRow(
                "MAC",
                info.macAddress.ifEmpty { "—" }
            )
            InfoDivider()
            InfoRow(
                "Msg Length",
                "${info.messageLength}"
            )
            InfoDivider()
            InfoRow(
                "Authentic",
                if (info.isFake) "Clone 🎭"
                else if (info.spoofedModel ==
                    uzair.lightpods.android
                        .bluetooth
                        .SpoofedModel.UNKNOWN
                ) "—"
                else "Likely ✓"
            )
        }
    }
}

@Composable
private fun GestureCard(
    gestures: List<GestureMapping>
) {
    SectionCard(title = "Touch Controls") {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        ) {
            gestures.forEachIndexed {
                    index, mapping ->
                InfoRow(
                    mapping.gesture,
                    mapping.action.displayName
                )
                if (index < gestures.size - 1) {
                    InfoDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
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
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color =
        MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme
                    .typography.bodyMedium,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme
                .typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
            color = valueColor
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme
            .outlineVariant.copy(alpha = 0.3f)
    )
}

@Composable
private fun DisconnectedContent(
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(
                R.drawable.case_closed
            ),
            contentDescription = "Case Closed",
            modifier = Modifier
                .size(160.dp)
                .alpha(0.5f),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Not Connected",
            style = MaterialTheme
                .typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
            color = MaterialTheme
                .colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Open case near your phone",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        PulsingDots()
        Spacer(modifier = Modifier.height(24.dp))
        PermissionStatusCard(
            modifier = Modifier.padding(
                horizontal = 16.dp
            )
        )
    }
}

@Composable
private fun ScanningContent(
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(
                R.drawable.case_open_buds
            ),
            contentDescription = "Scanning",
            modifier = Modifier.size(160.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Scanning\u2026",
            style = MaterialTheme
                .typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
            color = MaterialTheme
                .colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(32.dp))
        PulsingDots()
        Spacer(modifier = Modifier.height(24.dp))
        PermissionStatusCard(
            modifier = Modifier.padding(
                horizontal = 16.dp
            )
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
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 600,
                            delayMillis = index * 200,
                            easing = LinearEasing
                        ),
                        repeatMode =
                            RepeatMode.Reverse
                    ),
                    label = "dot$index"
                )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                    )
            )
        }
    }
}
