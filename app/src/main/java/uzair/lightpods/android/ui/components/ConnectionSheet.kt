package uzair.lightpods.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import uzair.lightpods.android.R
import uzair.lightpods.android.bluetooth.MicrophoneLocation
import uzair.lightpods.android.bluetooth.PodBattery
import uzair.lightpods.android.ui.theme.BatteryFull
import uzair.lightpods.android.ui.theme.BatteryLow
import uzair.lightpods.android.ui.theme.BatteryMedium
import uzair.lightpods.android.ui.theme.BatteryUnknown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSheet(
    deviceName: String,
    battery: PodBattery,
    micLocation: MicrophoneLocation,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var contentVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        delay(100)
        contentVisible = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = 32.dp,
            topEnd = 32.dp
        ),
        containerColor =
            MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        MaterialTheme.colorScheme
                            .outlineVariant
                    )
            )
        },
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio =
                        Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialOffsetY = { it / 4 }
            ) + fadeIn()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                ConnectionHero(
                    deviceName = deviceName,
                    battery = battery,
                    micLocation = micLocation
                )

                Spacer(modifier = Modifier.height(18.dp))

                BatterySummaryGrid(
                    battery = battery,
                    micLocation = micLocation
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme
                            .typography.labelLarge
                            .copy(
                                fontWeight =
                                    FontWeight.Bold
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionHero(
    deviceName: String,
    battery: PodBattery,
    micLocation: MicrophoneLocation
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
            .copy(alpha = 0.58f)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme
                                .primaryContainer
                                .copy(alpha = 0.86f),
                            MaterialTheme.colorScheme
                                .surfaceVariant
                                .copy(alpha = 0.38f)
                        )
                    )
                )
                .padding(18.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PodArt(
                    res = R.drawable.pod_left,
                    contentDescription = "Left pod",
                    modifier = Modifier.size(86.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                PodArt(
                    res = R.drawable.case_closed,
                    contentDescription = "Case",
                    modifier = Modifier.size(112.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                PodArt(
                    res = R.drawable.pod_right,
                    contentDescription = "Right pod",
                    modifier = Modifier.size(86.dp)
                )
            }

            Text(
                text = deviceName.ifBlank { "AirPods Pro" },
                style = MaterialTheme
                    .typography.headlineSmall
                    .copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connected - ${micLabel(micLocation)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                ConnectionChip(
                    label = bestBatteryLabel(battery),
                    color = bestBatteryColor(battery)
                )
                ConnectionChip(
                    label = "BLE live",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PodArt(
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
private fun ConnectionChip(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme
                    .typography.labelMedium
                    .copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun BatterySummaryGrid(
    battery: PodBattery,
    micLocation: MicrophoneLocation
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PopupBatteryRow(
            label = "Left pod",
            percent = battery.leftPercent,
            charging = battery.isLeftCharging,
            note = if (micLocation ==
                MicrophoneLocation.LEFT
            ) "Mic" else null
        )
        PopupBatteryRow(
            label = "Right pod",
            percent = battery.rightPercent,
            charging = battery.isRightCharging,
            note = if (micLocation ==
                MicrophoneLocation.RIGHT
            ) "Mic" else null
        )
        PopupBatteryRow(
            label = "Case",
            percent = battery.casePercent,
            charging = battery.isCaseCharging,
            note = null
        )
    }
}

@Composable
private fun PopupBatteryRow(
    label: String,
    percent: Int,
    charging: Boolean,
    note: String?
) {
    val isAvailable = percent in 0..100
    val color = batteryColor(percent)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
            .copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
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
                            .copy(
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                    )
                    if (charging) {
                        Text(
                            text = "Charging",
                            style = MaterialTheme
                                .typography.labelSmall,
                            color = MaterialTheme
                                .colorScheme.primary
                        )
                    }
                    if (note != null) {
                        Text(
                            text = note,
                            style = MaterialTheme
                                .typography.labelSmall,
                            color = MaterialTheme
                                .colorScheme.primary
                        )
                    }
                }
                Text(
                    text = if (isAvailable) {
                        "$percent%"
                    } else {
                        "Unknown"
                    },
                    style = MaterialTheme
                        .typography.labelLarge
                        .copy(fontWeight = FontWeight.Bold)
                )
            }
            LinearProgressIndicator(
                progress = {
                    if (isAvailable) percent / 100f else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme
                    .outlineVariant.copy(alpha = 0.42f)
            )
        }
    }
}

private fun micLabel(
    micLocation: MicrophoneLocation
): String {
    return when (micLocation) {
        MicrophoneLocation.LEFT -> "left microphone"
        MicrophoneLocation.RIGHT -> "right microphone"
        MicrophoneLocation.UNKNOWN -> "microphone unknown"
    }
}

private fun bestBatteryLabel(
    battery: PodBattery
): String {
    val available = listOf(
        battery.leftPercent,
        battery.rightPercent,
        battery.casePercent
    ).filter { it in 0..100 }

    return if (available.isEmpty()) {
        "Battery unknown"
    } else {
        "${available.min()}% minimum"
    }
}

@Composable
private fun bestBatteryColor(
    battery: PodBattery
): Color {
    val available = listOf(
        battery.leftPercent,
        battery.rightPercent,
        battery.casePercent
    ).filter { it in 0..100 }

    return batteryColor(available.minOrNull() ?: -1)
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
