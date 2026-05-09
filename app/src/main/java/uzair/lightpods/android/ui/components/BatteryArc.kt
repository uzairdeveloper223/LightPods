package uzair.lightpods.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uzair.lightpods.android.ui.theme.BatteryFull
import uzair.lightpods.android.ui.theme.BatteryLow
import uzair.lightpods.android.ui.theme.BatteryMedium
import uzair.lightpods.android.ui.theme.BatteryUnknown

@Composable
fun BatteryArc(
    percent: Int,
    label: String,
    modifier: Modifier = Modifier,
    arcSize: Dp = 80.dp,
    strokeWidth: Dp = 6.dp
) {
    val isAvailable = percent in 0..100
    val displayPercent = if (isAvailable) percent else 0

    val animatedSweep = remember { Animatable(0f) }
    LaunchedEffect(displayPercent) {
        animatedSweep.animateTo(
            targetValue = displayPercent / 100f * 270f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    val batteryColor = when {
        !isAvailable -> BatteryUnknown
        percent > 50 -> BatteryFull
        percent > 20 -> BatteryMedium
        else -> BatteryLow
    }

    val trackColor =
        MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.4f
        )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(arcSize)
        ) {
            Canvas(modifier = Modifier.size(arcSize)) {
                val strokePx = strokeWidth.toPx()
                val inset = strokePx / 2f
                val arcRect = Size(
                    size.width - strokePx,
                    size.height - strokePx
                )

                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcRect,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    )
                )

                if (isAvailable) {
                    drawArc(
                        color = batteryColor,
                        startAngle = 135f,
                        sweepAngle = animatedSweep.value,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcRect,
                        style = Stroke(
                            width = strokePx,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            Text(
                text = if (isAvailable) "$percent%" else "—",
                style = MaterialTheme.typography.titleMedium
                    .copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (
                            arcSize > 70.dp
                        ) 18.sp else 14.sp
                    ),
                color = if (isAvailable)
                    MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BatteryRow(
    leftPercent: Int,
    rightPercent: Int,
    casePercent: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BatteryArc(
            percent = leftPercent,
            label = "Left"
        )
        BatteryArc(
            percent = rightPercent,
            label = "Right"
        )
        BatteryArc(
            percent = casePercent,
            label = "Case"
        )
    }
}
