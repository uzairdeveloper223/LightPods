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
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    // ── Dead-pod battery cache (CaPod-inspired) ──
    // Persists across headset disconnects so that when
    // a pod dies and BLE goes silent, we can restore
    // the last known state instead of showing Unknown.
    // Only fully reset when the BT adapter turns off.
    private var lastKnownLeftPercent: Int = -1
    private var lastKnownRightPercent: Int = -1
    private var lastKnownCasePercent: Int = -1
    private var lastKnownLeftCharging: Boolean = false
    private var lastKnownRightCharging: Boolean = false
    private var lastKnownCaseCharging: Boolean = false

    // Timestamp of the last successful BLE scan update
    private var lastBleUpdateMs: Long = 0L
    // Staleness checker coroutine handle
    private var stalenessJob: Job? = null

    companion object {
        private const val TAG = "PodsManager"
        // If no BLE data for this long while headset
        // is connected, we consider data stale and
        // restore cached battery with dead inference.
        private const val BLE_STALE_TIMEOUT_MS = 15_000L
        // How often the staleness checker runs
        private const val STALE_CHECK_INTERVAL_MS = 5_000L

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
                    BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED ->
                        handleBatteryLevelChanged(intent)
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
            addAction(
                BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED
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
        startStalenessChecker()
    }

    /**
     * Periodic checker (CaPod stale-eviction pattern).
     * When headset is connected but BLE data has not
     * arrived for [BLE_STALE_TIMEOUT_MS], restore
     * cached battery values and infer dead pods.
     */
    private fun startStalenessChecker() {
        stalenessJob?.cancel()
        stalenessJob = scope.launch {
            while (true) {
                delay(STALE_CHECK_INTERVAL_MS)
                val now = System.currentTimeMillis()
                val currentState = _state.value

                // Only act when headset says connected
                // but BLE has gone silent.
                if (currentState.connectionState !=
                    ConnectionState.CONNECTED
                ) continue

                val bleAge = now - lastBleUpdateMs
                if (lastBleUpdateMs == 0L ||
                    bleAge < BLE_STALE_TIMEOUT_MS
                ) continue

                // BLE is stale. Restore cached battery.
                val hasCachedLeft =
                    lastKnownLeftPercent in 0..100
                val hasCachedRight =
                    lastKnownRightPercent in 0..100
                val hasCachedCase =
                    lastKnownCasePercent in 0..100
                val hasAnyCached =
                    hasCachedLeft || hasCachedRight ||
                        hasCachedCase

                if (!hasAnyCached) {
                    // First-time: never received valid
                    // BLE data. Headset is connected
                    // but BLE is silent → one or more
                    // pods may be dead.
                    Log.d(TAG,
                        "BLE stale, no cached data. " +
                            "Pods may be dead.")
                    _state.update { s ->
                        s.copy(
                            battery = PodBattery(
                                leftPercent = -1,
                                rightPercent = -1,
                                casePercent = -1,
                                isLeftDead = true,
                                isRightDead = true,
                                isCaseDead = false
                            )
                        )
                    }
                    continue
                }

                // We have cached data. Restore it.
                // If a pod had valid battery cached,
                // it was alive before but BLE went
                // silent → might still be alive
                // (broadcast just stopped). Show the
                // cached value. If we only had one pod
                // in cache, the other was never seen
                // → mark that one dead.
                val isLeftDead = !hasCachedLeft &&
                    hasCachedRight
                val isRightDead = !hasCachedRight &&
                    hasCachedLeft

                Log.d(TAG,
                    "BLE stale ${bleAge}ms. " +
                        "Restoring cache: " +
                        "L=$lastKnownLeftPercent " +
                        "R=$lastKnownRightPercent " +
                        "C=$lastKnownCasePercent")

                _state.update { s ->
                    s.copy(
                        battery = PodBattery(
                            leftPercent =
                                if (hasCachedLeft)
                                    lastKnownLeftPercent
                                else if (isLeftDead) 0
                                else -1,
                            rightPercent =
                                if (hasCachedRight)
                                    lastKnownRightPercent
                                else if (isRightDead) 0
                                else -1,
                            casePercent =
                                if (hasCachedCase)
                                    lastKnownCasePercent
                                else -1,
                            isLeftCharging =
                                if (hasCachedLeft)
                                    lastKnownLeftCharging
                                else false,
                            isRightCharging =
                                if (hasCachedRight)
                                    lastKnownRightCharging
                                else false,
                            isCaseCharging =
                                if (hasCachedCase)
                                    lastKnownCaseCharging
                                else false,
                            isLeftDead = isLeftDead,
                            isRightDead = isRightDead,
                            isCaseDead = false
                        )
                    )
                }
            }
        }
    }

    fun restartBleScan() {
        bleScanner.stopScanning()
        bleScanner.startScanning()
    }

    private fun mergeBleState(scan: BleScanState) {
        // Record that we got fresh BLE data
        lastBleUpdateMs = System.currentTimeMillis()

        val mic = when {
            scan.isLeftMicrophone ->
                MicrophoneLocation.LEFT
            else -> MicrophoneLocation.RIGHT
        }

        val detectedModel = scan.spoofedModel
        val deviceCodeHex = String.format(
            "0x%04X", scan.deviceModel
        )

        // ── Smart battery merge ──
        val live = scan.battery

        // Dead detection: a pod is dead if…
        // (a) it was alive before and now 0x0F, OR
        // (b) we’ve never seen it but the OTHER pod
        //     IS reporting → first-time dead.
        val isLeftDead = !live.isLeftAvailable && (
            lastKnownLeftPercent >= 0 ||
                live.isRightAvailable
            )
        val isRightDead = !live.isRightAvailable && (
            lastKnownRightPercent >= 0 ||
                live.isLeftAvailable
            )
        val isCaseDead = !live.isCaseAvailable &&
            lastKnownCasePercent >= 0

        // Update cache with fresh valid readings
        if (live.isLeftAvailable) {
            lastKnownLeftPercent = live.leftPercent
            lastKnownLeftCharging = live.isLeftCharging
        }
        if (live.isRightAvailable) {
            lastKnownRightPercent = live.rightPercent
            lastKnownRightCharging = live.isRightCharging
        }
        if (live.isCaseAvailable) {
            lastKnownCasePercent = live.casePercent
            lastKnownCaseCharging = live.isCaseCharging
        }

        val mergedBattery = PodBattery(
            leftPercent = when {
                live.isLeftAvailable -> live.leftPercent
                isLeftDead -> 0
                else -> -1
            },
            rightPercent = when {
                live.isRightAvailable ->
                    live.rightPercent
                isRightDead -> 0
                else -> -1
            },
            casePercent = when {
                live.isCaseAvailable -> live.casePercent
                isCaseDead -> 0
                else -> -1
            },
            isLeftCharging = if (live.isLeftAvailable)
                live.isLeftCharging else false,
            isRightCharging = if (live.isRightAvailable)
                live.isRightCharging else false,
            isCaseCharging = if (live.isCaseAvailable)
                live.isCaseCharging else false,
            isLeftDead = isLeftDead,
            isRightDead = isRightDead,
            isCaseDead = isCaseDead
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
                battery = mergedBattery,
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
                        (mergedBattery.isLeftAvailable ||
                            mergedBattery.isRightAvailable ||
                            isLeftDead || isRightDead)
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

    private fun resetBatteryCache() {
        lastKnownLeftPercent = -1
        lastKnownRightPercent = -1
        lastKnownCasePercent = -1
        lastKnownLeftCharging = false
        lastKnownRightCharging = false
        lastKnownCaseCharging = false
        lastBleUpdateMs = 0L
    }

    @SuppressLint("MissingPermission")
    private fun handleDeviceConnected(
        device: BluetoothDevice
    ) {
        val deviceName = device.name ?: "LightPods"
        val macAddress = device.address ?: ""

        val systemBattery = try {
            device.getBatteryLevel()
        } catch (e: Exception) {
            -1
        }
        Log.d(TAG, "handleDeviceConnected: device=${device.address}, systemBattery=$systemBattery")

        var leftVal = lastKnownLeftPercent
        var rightVal = lastKnownRightPercent

        if (leftVal < 0 && systemBattery in 0..100) {
            leftVal = systemBattery
            lastKnownLeftPercent = systemBattery
        }
        if (rightVal < 0 && systemBattery in 0..100) {
            rightVal = systemBattery
            lastKnownRightPercent = systemBattery
        }

        // Restore cached battery on reconnect so
        // the user doesn't see all-Unknown after a
        // brief disconnect/reconnect cycle.
        val hasCachedLeft = leftVal in 0..100
        val hasCachedRight = rightVal in 0..100
        val hasCachedCase = lastKnownCasePercent in 0..100

        _state.update { current ->
            val wasDisconnected =
                current.connectionState !=
                    ConnectionState.CONNECTED
            val shouldShowPopup =
                wasDisconnected &&
                    !current.popupDismissedWhileDisconnected

            // If current battery is all unknown but
            // we have cached data, restore it.
            val needsRestore =
                !current.battery.isLeftAvailable &&
                    !current.battery.isRightAvailable &&
                    (hasCachedLeft || hasCachedRight)

            val restoredBattery = if (needsRestore) {
                PodBattery(
                    leftPercent =
                        if (hasCachedLeft)
                            leftVal
                        else 0,
                    rightPercent =
                        if (hasCachedRight)
                            rightVal
                        else 0,
                    casePercent =
                        if (hasCachedCase)
                            lastKnownCasePercent
                        else -1,
                    isLeftCharging =
                        if (hasCachedLeft)
                            lastKnownLeftCharging
                        else false,
                    isRightCharging =
                        if (hasCachedRight)
                            lastKnownRightCharging
                        else false,
                    isCaseCharging =
                        if (hasCachedCase)
                            lastKnownCaseCharging
                        else false,
                    isLeftDead = !hasCachedLeft &&
                        hasCachedRight,
                    isRightDead = !hasCachedRight &&
                        hasCachedLeft,
                    isCaseDead = false
                )
            } else {
                current.battery
            }

            current.copy(
                connectionState =
                    ConnectionState.CONNECTED,
                battery = restoredBattery,
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
                // Do NOT reset battery cache here!
                // We preserve it so that when the
                // surviving pod reconnects we can
                // restore the last known state and
                // detect the dead pod.
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
                resetBatteryCache()
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

    @SuppressLint("MissingPermission")
    private fun handleBatteryLevelChanged(intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return

        val batteryLevel = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
        Log.d(TAG, "handleBatteryLevelChanged: device=${device.address}, level=$batteryLevel")
        if (batteryLevel !in 0..100) return

        val now = System.currentTimeMillis()
        val bleAge = now - lastBleUpdateMs
        val bleIsStale = lastBleUpdateMs == 0L || bleAge > BLE_STALE_TIMEOUT_MS

        if (bleIsStale) {
            lastKnownLeftPercent = batteryLevel
            lastKnownRightPercent = batteryLevel
            
            _state.update { current ->
                val deviceName = device.name ?: current.deviceInfo.deviceName
                val macAddress = device.address
                
                current.copy(
                    connectionState = ConnectionState.CONNECTED,
                    battery = current.battery.copy(
                        leftPercent = batteryLevel,
                        rightPercent = batteryLevel,
                        isLeftDead = false,
                        isRightDead = false
                    ),
                    deviceInfo = current.deviceInfo.copy(
                        deviceName = deviceName,
                        macAddress = macAddress
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

}
