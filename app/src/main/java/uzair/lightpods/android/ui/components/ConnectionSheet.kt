package uzair.lightpods.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        delay(150)
        contentVisible = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = 28.dp, topEnd = 28.dp
        ),
        containerColor =
            MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        dragHandle = {
            Spacer(modifier = Modifier.height(8.dp))
        },
        modifier = modifier
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
            AnimatedVisibility(
                visible = contentVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio =
                            Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialOffsetY = { it / 3 }
                ) + fadeIn()
            ) {
                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    DeviceHeroRow(battery, micLocation)

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = deviceName,
                        style = MaterialTheme
                            .typography.headlineSmall
                            .copy(
                                fontWeight =
                                    FontWeight.Bold
                            ),
                        color = MaterialTheme
                            .colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "AirPods Pro \u00B7 Connected",
                        style = MaterialTheme
                            .typography.bodyMedium,
                        color = MaterialTheme
                            .colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme
                                .typography.labelLarge
                                .copy(
                                    fontWeight =
                                        FontWeight
                                            .SemiBold
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceHeroRow(
    battery: PodBattery,
    micLocation: MicrophoneLocation
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.Center,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        PodWithBattery(
            imageRes = R.drawable.pod_left,
            label = "Left",
            percent = battery.leftPercent,
            isCharging = battery.isLeftCharging,
            hasMic = micLocation ==
                MicrophoneLocation.LEFT
        )

        Spacer(modifier = Modifier.width(4.dp))

        PodWithBattery(
            imageRes = R.drawable.pod_right,
            label = "Right",
            percent = battery.rightPercent,
            isCharging = battery.isRightCharging,
            hasMic = micLocation ==
                MicrophoneLocation.RIGHT
        )

        Spacer(modifier = Modifier.width(8.dp))

        CaseWithBattery(
            percent = battery.casePercent,
            isCharging = battery.isCaseCharging
        )
    }
}

@Composable
private fun PodWithBattery(
    imageRes: Int,
    label: String,
    percent: Int,
    isCharging: Boolean,
    hasMic: Boolean
) {
    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(140.dp)
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = label,
                modifier = Modifier.size(90.dp),
                contentScale = ContentScale.Fit
            )
        }

        if (hasMic) {
            Text(
                text = "\uD83C\uDFA4",
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (percent in 0..100)
                "$percent%" else "—",
            style = MaterialTheme.typography.labelLarge
                .copy(fontWeight = FontWeight.Bold),
            color = batteryColor(percent)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant
        )

        if (isCharging) {
            Text(
                text = "\u26A1",
                fontSize = 10.sp,
                modifier = Modifier.offset(y = (-2).dp)
            )
        }
    }
}

@Composable
private fun CaseWithBattery(
    percent: Int,
    isCharging: Boolean
) {
    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(140.dp)
        ) {
            Image(
                painter = painterResource(
                    R.drawable.case_closed
                ),
                contentDescription = "Case",
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (percent in 0..100)
                "$percent%" else "—",
            style = MaterialTheme.typography.labelLarge
                .copy(fontWeight = FontWeight.Bold),
            color = batteryColor(percent)
        )

        Text(
            text = "Case",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant
        )

        if (isCharging) {
            Text(
                text = "\u26A1",
                fontSize = 10.sp,
                modifier = Modifier.offset(y = (-2).dp)
            )
        }
    }
}

@Composable
private fun batteryColor(
    percent: Int
): androidx.compose.ui.graphics.Color {
    return when {
        percent !in 0..100 -> BatteryUnknown
        percent > 50 -> BatteryFull
        percent > 20 -> BatteryMedium
        else -> BatteryLow
    }
}
