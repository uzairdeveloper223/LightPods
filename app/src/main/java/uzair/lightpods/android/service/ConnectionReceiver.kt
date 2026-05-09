package uzair.lightpods.android.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Manifest-registered receiver that catches system
 * events even when the app process is dead:
 * - BOOT_COMPLETED → restart monitoring service
 * - ACL_CONNECTED  → device paired, start service
 * - CONNECTION_STATE_CHANGED → headset connected
 * - BLUETOOTH_ON   → BT turned on, start scanning
 *
 * This ensures the overlay popup fires even if the
 * user swiped the app away and Android killed it.
 */
class ConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothHeadset
                .ACTION_CONNECTION_STATE_CHANGED -> {
                ensureServiceRunning(context)
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.STATE_OFF
                )
                if (state == BluetoothAdapter.STATE_ON) {
                    ensureServiceRunning(context)
                }
            }
        }
    }

    private fun ensureServiceRunning(
        context: Context
    ) {
        try {
            PodsMonitorService.start(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
