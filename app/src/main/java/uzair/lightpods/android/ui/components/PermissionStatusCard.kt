package uzair.lightpods.android.ui.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

data class PermissionStatus(
    val label: String,
    val granted: Boolean,
    val fixAction: (() -> Unit)? = null
)

@Composable
fun PermissionStatusCard(
    modifier: Modifier = Modifier,
    onRequestNotificationPermission: (() -> Unit)? = null,
    showGrantedStatuses: Boolean = false
) {
    val context = LocalContext.current
    var statuses by remember {
        mutableStateOf(
            checkAllStatuses(
                context,
                onRequestNotificationPermission
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            statuses = checkAllStatuses(
                context,
                onRequestNotificationPermission
            )
        }
    }

    val missingStatuses = statuses.filterNot {
        it.granted
    }
    val visibleStatuses = if (showGrantedStatuses) {
        statuses
    } else {
        missingStatuses
    }
    if (visibleStatuses.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme
                    .errorContainer.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = if (showGrantedStatuses) {
                    "Permission status"
                } else {
                    "Needs attention"
                },
                style = MaterialTheme
                    .typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                color = MaterialTheme
                    .colorScheme.onSurface
            )
            Text(
                text = if (showGrantedStatuses) {
                    "Green items are ready. Red items still need permission."
                } else {
                    "Enable the missing permissions for live scanning and popups."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            visibleStatuses.forEach { status ->
                StatusRow(status)
            }
        }
    }
}

@Composable
private fun StatusRow(status: PermissionStatus) {
    val dotColor by animateColorAsState(
        targetValue = if (status.granted)
            Color(0xFF6DD58C)
        else Color(0xFFFF6B6B),
        label = "dotColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!status.granted &&
                    status.fixAction != null
                ) {
                    Modifier.clickable {
                        status.fixAction.invoke()
                    }
                } else Modifier
            )
            .padding(vertical = 6.dp),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = status.label,
                style = MaterialTheme
                    .typography.bodyMedium,
                color = if (status.granted)
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
                else MaterialTheme.colorScheme
                    .onSurface
            )
        }

        if (!status.granted) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme
                    .primary.copy(alpha = 0.12f),
                contentColor =
                    MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Fix",
                    style = MaterialTheme
                        .typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    )
                )
            }
        }
    }
}

private fun checkAllStatuses(
    context: Context,
    onRequestNotificationPermission: (() -> Unit)?
): List<PermissionStatus> {
    val statuses = mutableListOf<PermissionStatus>()

    val btManager = context.getSystemService(
        Context.BLUETOOTH_SERVICE
    ) as? BluetoothManager
    val btEnabled =
        btManager?.adapter?.isEnabled == true

    statuses.add(
        PermissionStatus(
            label = "Bluetooth",
            granted = btEnabled,
            fixAction = {
                context.startActivity(
                    Intent(
                        Settings
                            .ACTION_BLUETOOTH_SETTINGS
                    )
                )
            }
        )
    )

    if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.S
    ) {
        val hasBtConnect = hasPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val hasBtScan = hasPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        )
        statuses.add(
            PermissionStatus(
                label = "Bluetooth Permission",
                granted = hasBtConnect && hasBtScan,
                fixAction = {
                    openAppSettings(context)
                }
            )
        )
    }

    val hasLocation = hasPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    statuses.add(
        PermissionStatus(
            label = "Location Permission",
            granted = hasLocation,
            fixAction = {
                openAppSettings(context)
            }
        )
    )

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
    val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager?.isLocationEnabled == true
    } else {
        @Suppress("DEPRECATION")
        locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
    }
    statuses.add(
        PermissionStatus(
            label = "Location Services (GPS)",
            granted = isLocationEnabled,
            fixAction = {
                context.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            }
        )
    )

    if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.Q
    ) {
        val hasBgLocation = hasPermission(
            context,
            Manifest.permission
                .ACCESS_BACKGROUND_LOCATION
        )
        statuses.add(
            PermissionStatus(
                label = "Background Location",
                granted = hasBgLocation,
                fixAction = {
                    openAppSettings(context)
                }
            )
        )
    }

    if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.M
    ) {
        val hasOverlay = Settings.canDrawOverlays(
            context
        )
        statuses.add(
            PermissionStatus(
                label = "Overlay",
                granted = hasOverlay,
                fixAction = {
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
            )
        )
    }

    if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU
    ) {
        val hasNotif = hasPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )
        statuses.add(
            PermissionStatus(
                label = "Notifications",
                granted = hasNotif,
                fixAction = {
                    onRequestNotificationPermission
                        ?.invoke()
                        ?: openAppSettings(context)
                }
            )
        )
    }

    return statuses
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
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse(
                "package:" + context.packageName
            )
        )
    )
}
