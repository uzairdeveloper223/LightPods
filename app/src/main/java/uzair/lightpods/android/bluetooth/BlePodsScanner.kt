package uzair.lightpods.android.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BlePodsScanner(private val context: Context) {

    private val _scanState = MutableStateFlow(BleScanState())
    val scanState: StateFlow<BleScanState> =
        _scanState.asStateFlow()

    /** All nearby devices keyed by MAC address with timestamps */
    private val _nearbyDevices = MutableStateFlow<Map<String, BleScanState>>(emptyMap())
    val nearbyDevices: StateFlow<Map<String, BleScanState>> =
        _nearbyDevices.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? =
        null
    private var isScanning = false

    private val scanCallback =
        object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {
                parseAppleProximityData(result)
            }

            override fun onBatchScanResults(
                results: MutableList<ScanResult>
            ) {
                results.forEach {
                    parseAppleProximityData(it)
                }
            }
        }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (isScanning) return

        val manager = context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as? BluetoothManager ?: return
        bluetoothAdapter = manager.adapter ?: return

        val scanner =
            bluetoothAdapter?.bluetoothLeScanner
                ?: return

        val manufacturerData =
            ByteArray(PROXIMITY_DATA_LENGTH).apply {
                this[0] =
                    PROXIMITY_PAIRING_TYPE.toByte()
            }
        val manufacturerMask =
            ByteArray(PROXIMITY_DATA_LENGTH).apply {
                this[0] = 0xFF.toByte()
            }

        val filter = ScanFilter.Builder()
            .setManufacturerData(
                APPLE_COMPANY_ID,
                manufacturerData,
                manufacturerMask
            )
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(
                ScanSettings.SCAN_MODE_LOW_LATENCY
            )
            .setReportDelay(0)
            .build()

        scanner.startScan(
            listOf(filter), settings, scanCallback
        )
        isScanning = true
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return
        try {
            bluetoothAdapter?.bluetoothLeScanner
                ?.stopScan(scanCallback)
        } catch (_: Exception) {
            // BT adapter may have been turned off
        }
        isScanning = false
    }

    private fun parseAppleProximityData(
        result: ScanResult
    ) {
        val record = result.scanRecord ?: return
        val appleData =
            record.getManufacturerSpecificData(
                APPLE_COMPANY_ID
            ) ?: return

        if (appleData.size < MIN_PAYLOAD_LENGTH) return
        if (appleData[0].toInt() and 0xFF !=
            PROXIMITY_PAIRING_TYPE
        ) return

        // Message length = byte[1]
        val messageLength =
            appleData[1].toInt() and 0xFF

        val dataOffset = 2
        if (appleData.size < dataOffset + 9) return

        // ── Byte layout (CAPod protocol) ──
        // [0] prefix
        // [1][2] device model code (16-bit)
        // [3] status byte
        // [4] pods battery (high/low nibble)
        // [5] flags(high) + case battery(low)
        // [6] lid state
        // [7] device color
        // [8] suffix

        val deviceCode =
            ((appleData[dataOffset + 1].toInt()
                and 0xFF) shl 8) or
                (appleData[dataOffset + 2].toInt()
                    and 0xFF)

        val statusByte =
            appleData[dataOffset + 3].toInt() and 0xFF
        val podsBatteryByte =
            appleData[dataOffset + 4].toInt() and 0xFF
        val flagsAndCaseByte =
            appleData[dataOffset + 5].toInt() and 0xFF
        val lidStateByte =
            appleData[dataOffset + 6].toInt() and 0xFF

        // ── Status bit parsing ──
        val isFlipped =
            (statusByte and (1 shl 5)) == 0
        val isThisPodInCase =
            (statusByte and (1 shl 6)) != 0
        val isOnePodInCase =
            (statusByte and (1 shl 4)) != 0
        val areBothInCase =
            (statusByte and (1 shl 2)) != 0

        // ── Battery (nibble-based, 0-10 scale) ──
        val rawLeftNibble: Int
        val rawRightNibble: Int
        if (isFlipped) {
            rawLeftNibble =
                (podsBatteryByte shr 4) and 0x0F
            rawRightNibble =
                podsBatteryByte and 0x0F
        } else {
            rawLeftNibble =
                podsBatteryByte and 0x0F
            rawRightNibble =
                (podsBatteryByte shr 4) and 0x0F
        }

        val leftBattery =
            nibbleToBattery(rawLeftNibble)
        val rightBattery =
            nibbleToBattery(rawRightNibble)

        val caseBatteryNibble =
            flagsAndCaseByte and 0x0F
        val caseBattery =
            nibbleToBattery(caseBatteryNibble)

        // ── Ear detection ──
        val isLeftInEar: Boolean
        val isRightInEar: Boolean
        if (isFlipped xor isThisPodInCase) {
            isLeftInEar =
                (statusByte and (1 shl 3)) != 0
            isRightInEar =
                (statusByte and (1 shl 1)) != 0
        } else {
            isLeftInEar =
                (statusByte and (1 shl 1)) != 0
            isRightInEar =
                (statusByte and (1 shl 3)) != 0
        }

        // ── Microphone ──
        val isLeftMic =
            ((statusByte and (1 shl 5)) != 0) xor
                isThisPodInCase

        // ── Case lid state ──
        val hasCaseContext = isThisPodInCase ||
            isOnePodInCase || areBothInCase
        val caseLidState: CaseLidState =
            if (!hasCaseContext) {
                CaseLidState.NOT_IN_CASE
            } else {
                when ((lidStateByte shr 3) and 0x01) {
                    0 -> CaseLidState.OPEN
                    1 -> CaseLidState.CLOSED
                    else -> CaseLidState.UNKNOWN
                }
            }
        val isCaseLidOpen =
            caseLidState == CaseLidState.OPEN

        // ── Model detection ──
        val spoofedModel = SpoofedModel.detect(
            deviceCode, messageLength
        )

        val rssi = result.rssi
        @SuppressLint("MissingPermission")
        val address = result.device?.address ?: ""

        val scanEntry = BleScanState(
            isDeviceFound = true,
            address = address,
            battery = PodBattery(
                leftPercent = leftBattery,
                rightPercent = rightBattery,
                casePercent = caseBattery
            ),
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isCaseLidOpen = isCaseLidOpen,
            caseLidState = caseLidState,
            isLeftMicrophone = isLeftMic,
            rssi = rssi,
            deviceModel = deviceCode,
            spoofedModel = spoofedModel,
            messageLength = messageLength,
            areBothPodsInCase = areBothInCase,
            isOnePodInCase = isOnePodInCase,
            isThisPodInCase = isThisPodInCase,
            lastSeenMs = System.currentTimeMillis()
        )

        // Update primary (strongest signal) device
        _scanState.value = scanEntry

        // Track ALL devices by MAC address
        if (address.isNotBlank()) {
            _nearbyDevices.update { map ->
                val pruned = map.filterValues {
                    System.currentTimeMillis() - it.lastSeenMs < STALE_TIMEOUT_MS
                }
                pruned + (address to scanEntry)
            }
        }
    }

    private fun nibbleToBattery(nibble: Int): Int {
        if (nibble == 0x0F) return -1
        val clamped = nibble.coerceAtMost(10)
        return clamped * 10
    }

    companion object {
        const val APPLE_COMPANY_ID = 0x004C
        const val PROXIMITY_PAIRING_TYPE = 0x07
        const val PROXIMITY_DATA_LENGTH = 27
        const val MIN_PAYLOAD_LENGTH = 10
        private const val STALE_TIMEOUT_MS = 10_000L  // 10 seconds
    }
}

data class BleScanState(
    val isDeviceFound: Boolean = false,
    val address: String = "",
    val battery: PodBattery = PodBattery(),
    val isLeftInEar: Boolean = false,
    val isRightInEar: Boolean = false,
    val isCaseLidOpen: Boolean = false,
    val caseLidState: CaseLidState =
        CaseLidState.UNKNOWN,
    val isLeftMicrophone: Boolean = false,
    val rssi: Int = 0,
    val deviceModel: Int = 0,
    val spoofedModel: SpoofedModel =
        SpoofedModel.UNKNOWN,
    val messageLength: Int = 0,
    val areBothPodsInCase: Boolean = false,
    val isOnePodInCase: Boolean = false,
    val isThisPodInCase: Boolean = false,
    val lastSeenMs: Long = 0L
)
