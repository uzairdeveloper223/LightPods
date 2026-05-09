package uzair.lightpods.android.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BluetoothPodsManager private constructor(
    private val context: Context
) {

    private val _state = MutableStateFlow(PodsUiState())
    val state: StateFlow<PodsUiState> = _state.asStateFlow()

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var headsetProfile: BluetoothHeadset? = null
    private val bleScanner = BlePodsScanner(context)
    private var isInitialized = false

    private val profileListener =
        object : BluetoothProfile.ServiceListener {
            @SuppressLint("MissingPermission")
            override fun onServiceConnected(
                profile: Int,
                proxy: BluetoothProfile
            ) {
                if (profile != BluetoothProfile.HEADSET) return
                headsetProfile = proxy as BluetoothHeadset
                val devices = proxy.connectedDevices
                if (devices.isNotEmpty()) {
                    handleDeviceConnected(devices[0])
                }
            }

            override fun onServiceDisconnected(
                profile: Int
            ) {
                if (profile != BluetoothProfile.HEADSET) return
                headsetProfile = null
            }
        }

    private val connectionReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                ctx: Context,
                intent: Intent
            ) {
                when (intent.action) {
                    BluetoothHeadset
                        .ACTION_CONNECTION_STATE_CHANGED ->
                        handleConnectionIntent(intent)
                    BluetoothHeadset
                        .ACTION_VENDOR_SPECIFIC_HEADSET_EVENT ->
                        handleVendorEvent(intent)
                    BluetoothAdapter
                        .ACTION_STATE_CHANGED ->
                        handleAdapterStateChange(intent)
                }
            }
        }

    @SuppressLint("MissingPermission")
    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        val manager = context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as? BluetoothManager ?: return
        bluetoothAdapter = manager.adapter ?: return

        bluetoothAdapter?.getProfileProxy(
            context,
            profileListener,
            BluetoothProfile.HEADSET
        )

        val filter = IntentFilter().apply {
            addAction(
                BluetoothHeadset
                    .ACTION_CONNECTION_STATE_CHANGED
            )
            addAction(
                BluetoothHeadset
                    .ACTION_VENDOR_SPECIFIC_HEADSET_EVENT
            )
            addAction(
                BluetoothAdapter.ACTION_STATE_CHANGED
            )
        }

        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            context.registerReceiver(
                connectionReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                connectionReceiver, filter
            )
        }

        startBleScan()

        _state.update {
            it.copy(
                connectionState =
                    ConnectionState.SCANNING
            )
        }
    }

    fun release() {
        bleScanner.stopScanning()
        try {
            context.unregisterReceiver(connectionReceiver)
        } catch (_: IllegalArgumentException) { }

        headsetProfile?.let {
            bluetoothAdapter?.closeProfileProxy(
                BluetoothProfile.HEADSET, it
            )
        }
        headsetProfile = null
        isInitialized = false
    }

    private fun startBleScan() {
        bleScanner.startScanning()
        scope.launch {
            bleScanner.scanState.collect { scan ->
                if (!scan.isDeviceFound) return@collect
                mergeBleState(scan)
            }
        }
    }

    fun restartBleScan() {
        bleScanner.stopScanning()
        bleScanner.startScanning()
    }

    private fun mergeBleState(scan: BleScanState) {
        val mic = when {
            scan.isLeftMicrophone ->
                MicrophoneLocation.LEFT
            else -> MicrophoneLocation.RIGHT
        }

        val detectedModel = scan.spoofedModel
        val deviceCodeHex = String.format(
            "0x%04X", scan.deviceModel
        )

        _state.update { current ->
            val wasDisconnected =
                current.connectionState !=
                    ConnectionState.CONNECTED
            val shouldShowPopup =
                wasDisconnected &&
                    !current.popupDismissedWhileDisconnected

            current.copy(
                connectionState =
                    ConnectionState.CONNECTED,
                battery = scan.battery,
                isLeftInEar = scan.isLeftInEar,
                isRightInEar = scan.isRightInEar,
                isCaseLidOpen = scan.isCaseLidOpen,
                caseLidState = scan.caseLidState,
                areBothPodsInCase =
                    scan.areBothPodsInCase,
                micLocation = mic,
                rssi = scan.rssi,
                showConnectionSheet =
                    if (shouldShowPopup &&
                        (scan.battery.isLeftAvailable ||
                            scan.battery.isRightAvailable)
                    ) true
                    else current.showConnectionSheet,
                deviceInfo = PodDeviceInfo(
                    deviceName =
                        current.deviceInfo.deviceName
                            .ifEmpty {
                                detectedModel.label
                            },
                    macAddress = scan.address
                        .ifEmpty {
                            current.deviceInfo.macAddress
                        },
                    spoofedModel = detectedModel,
                    rawDeviceCode = scan.deviceModel,
                    rawDeviceCodeHex = deviceCodeHex,
                    messageLength =
                        scan.messageLength,
                    isFake = detectedModel.isFake
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleDeviceConnected(
        device: BluetoothDevice
    ) {
        val deviceName = device.name ?: "LightPods"
        val macAddress = device.address ?: ""

        _state.update { current ->
            val wasDisconnected =
                current.connectionState !=
                    ConnectionState.CONNECTED
            val shouldShowPopup =
                wasDisconnected &&
                    !current.popupDismissedWhileDisconnected

            current.copy(
                connectionState =
                    ConnectionState.CONNECTED,
                deviceInfo = PodDeviceInfo(
                    deviceName = deviceName,
                    macAddress = macAddress,
                    spoofedModel =
                        current.deviceInfo.spoofedModel,
                    rawDeviceCode =
                        current.deviceInfo.rawDeviceCode,
                    rawDeviceCodeHex =
                        current.deviceInfo.rawDeviceCodeHex,
                    messageLength =
                        current.deviceInfo.messageLength,
                    isFake =
                        current.deviceInfo.isFake
                ),
                showConnectionSheet = shouldShowPopup,
                popupDismissedWhileDisconnected = false
            )
        }
    }

    private fun handleConnectionIntent(intent: Intent) {
        val btState = intent.getIntExtra(
            BluetoothHeadset.EXTRA_STATE,
            BluetoothHeadset.STATE_DISCONNECTED
        )

        when (btState) {
            BluetoothHeadset.STATE_CONNECTED -> {
                val device =
                    if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.TIRAMISU
                    ) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE
                        )
                    }
                device?.let { handleDeviceConnected(it) }
            }
            BluetoothHeadset.STATE_DISCONNECTED -> {
                _state.update { current ->
                    current.copy(
                        connectionState =
                            ConnectionState.DISCONNECTED,
                        showConnectionSheet = false,
                        popupDismissedWhileDisconnected =
                            false
                    )
                }
            }
        }
    }

    private fun handleAdapterStateChange(
        intent: Intent
    ) {
        val adapterState = intent.getIntExtra(
            BluetoothAdapter.EXTRA_STATE,
            BluetoothAdapter.STATE_OFF
        )
        when (adapterState) {
            BluetoothAdapter.STATE_ON -> {
                restartBleScan()
                _state.update {
                    it.copy(
                        connectionState =
                            ConnectionState.SCANNING
                    )
                }
            }
            BluetoothAdapter.STATE_OFF -> {
                bleScanner.stopScanning()
                _state.update {
                    it.copy(
                        connectionState =
                            ConnectionState.DISCONNECTED
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun handleVendorEvent(intent: Intent) {
        val command = intent.getStringExtra(
            BluetoothHeadset
                .EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD
        ) ?: return

        if (!command.contains("IPHONEACCEV")) return

        val args = intent.extras?.get(
            BluetoothHeadset
                .EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS
        )
        parseBatteryFromArgs(args)
    }

    private fun parseBatteryFromArgs(args: Any?) {
        if (args == null) return
        val values: List<Int> = when (args) {
            is Array<*> -> args.mapNotNull {
                (it as? Int)
                    ?: it?.toString()?.toIntOrNull()
            }
            is IntArray -> args.toList()
            else -> return
        }
        if (values.size < 3) return
        val pairCount = values[0]
        var idx = 1
        var batteryLevel = -1
        repeat(pairCount) {
            if (idx + 1 >= values.size) return@repeat
            val key = values[idx]
            val value = values[idx + 1]
            idx += 2
            if (key == 1) {
                batteryLevel = (value + 1) * 10
            }
        }
        if (batteryLevel !in 0..100) return

        _state.update { current ->
            if (current.battery.isLeftAvailable) {
                current
            } else {
                current.copy(
                    battery = current.battery.copy(
                        leftPercent = batteryLevel,
                        rightPercent = batteryLevel,
                        casePercent =
                            (batteryLevel * 0.9).toInt()
                    )
                )
            }
        }
    }

    fun dismissConnectionSheet() {
        _state.update {
            it.copy(
                showConnectionSheet = false,
                popupDismissedWhileDisconnected =
                    it.connectionState !=
                        ConnectionState.CONNECTED
            )
        }
    }

    fun connectForDemo() {
        _state.update { current ->
            current.copy(
                connectionState =
                    ConnectionState.CONNECTED,
                battery = PodBattery(
                    leftPercent = 90,
                    rightPercent = 85,
                    casePercent = 72,
                    isLeftCharging = false,
                    isRightCharging = false,
                    isCaseCharging = true
                ),
                deviceInfo = PodDeviceInfo(
                    deviceName = "LightPods",
                    macAddress = "41:42:F1:B4:A2:E0"
                ),
                showConnectionSheet = true,
                popupDismissedWhileDisconnected = false,
                isCaseLidOpen = true,
                isLeftInEar = false,
                isRightInEar = false,
                micLocation = MicrophoneLocation.LEFT
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BluetoothPodsManager? = null

        fun getInstance(context: Context): BluetoothPodsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothPodsManager(
                    context.applicationContext
                ).also { INSTANCE = it }
            }
        }
    }
}
