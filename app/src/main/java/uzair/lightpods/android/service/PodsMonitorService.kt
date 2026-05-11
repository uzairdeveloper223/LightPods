package uzair.lightpods.android.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import uzair.lightpods.android.MainActivity
import uzair.lightpods.android.R
import uzair.lightpods.android.bluetooth.BluetoothPodsManager
import uzair.lightpods.android.bluetooth.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PodsMonitorService : Service() {

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var podsManager: BluetoothPodsManager? = null
    private var overlayManager: OverlayPopupManager? = null
    private var wasConnected = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification(
            "LightPods",
            "Scanning for nearby pods…"
        )
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        overlayManager = OverlayPopupManager(this)

        podsManager = BluetoothPodsManager
            .getInstance(applicationContext)
        podsManager?.initialize()

        startMonitoring()
    }

    private fun startMonitoring() {
        serviceScope.launch {
            podsManager?.state?.collect { state ->
                val nm = getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

                when (state.connectionState) {
                    ConnectionState.CONNECTED -> {
                        val b = state.battery
                        val parts =
                            mutableListOf<String>()
                        if (b.isLeftDead) {
                            parts.add("L: Dead")
                        } else if (b.isLeftAvailable) {
                            parts.add(
                                "L: ${b.leftPercent}%"
                            )
                        }
                        if (b.isRightDead) {
                            parts.add("R: Dead")
                        } else if (b.isRightAvailable) {
                            parts.add(
                                "R: ${b.rightPercent}%"
                            )
                        }
                        if (b.isCaseDead) {
                            parts.add("Case: Dead")
                        } else if (b.isCaseAvailable) {
                            parts.add(
                                "Case: ${b.casePercent}%"
                            )
                        }

                        val batteryText =
                            if (parts.isNotEmpty())
                                parts.joinToString(" · ")
                            else "Connected"

                        val subtitle = batteryText

                        val name =
                            state.deviceInfo.deviceName
                                .ifEmpty { "LightPods" }

                        nm.notify(
                            NOTIFICATION_ID,
                            buildNotification(
                                name, subtitle
                            )
                        )

                        if (!wasConnected) {
                            overlayManager
                                ?.resetDismissState()
                            val scan =
                                uzair.lightpods.android
                                    .bluetooth
                                    .BleScanState(
                                    isDeviceFound = true,
                                    battery = b,
                                    isLeftMicrophone =
                                        state.micLocation ==
                                            uzair.lightpods
                                                .android
                                                .bluetooth
                                                .MicrophoneLocation
                                                .LEFT
                                )
                            overlayManager
                                ?.showConnectionPopup(
                                    scan, name
                                )
                        }
                        wasConnected = true
                    }
                    ConnectionState.SCANNING -> {
                        nm.notify(
                            NOTIFICATION_ID,
                            buildNotification(
                                "LightPods",
                                "Scanning for nearby pods…"
                            )
                        )
                        if (wasConnected) {
                            overlayManager?.onDeviceLost()
                        }
                        wasConnected = false
                    }
                    ConnectionState.DISCONNECTED -> {
                        nm.notify(
                            NOTIFICATION_ID,
                            buildNotification(
                                "LightPods",
                                "Monitoring · No pods nearby"
                            )
                        )
                        if (wasConnected) {
                            overlayManager?.onDeviceLost()
                        }
                        wasConnected = false
                    }
                }
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        podsManager?.initialize()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        overlayManager?.dismissPopup()
        overlayManager = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LightPods Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                "Battery & connection status"
            setShowBadge(false)
        }
        val manager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("LaunchActivityFromNotification")
    private fun buildNotification(
        title: String,
        text: String
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .apply {
                    flags =
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(
            this, CHANNEL_ID
        )
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(
                R.drawable.ic_launcher_foreground
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "lightpods_monitor"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(
                context,
                PodsMonitorService::class.java
            )
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(
                Intent(
                    context,
                    PodsMonitorService::class.java
                )
            )
        }
    }
}
