package uzair.lightpods.android.bluetooth

data class PodBattery(
    val leftPercent: Int = -1,
    val rightPercent: Int = -1,
    val casePercent: Int = -1,
    val isLeftCharging: Boolean = false,
    val isRightCharging: Boolean = false,
    val isCaseCharging: Boolean = false
) {
    val isLeftAvailable: Boolean
        get() = leftPercent in 0..100
    val isRightAvailable: Boolean
        get() = rightPercent in 0..100
    val isCaseAvailable: Boolean
        get() = casePercent in 0..100
}

/**
 * Spoofed model detection based on the BLE
 * proximity pairing device code (bytes 1-2).
 * Maps are derived from CAPod's AppleFactory.
 */
enum class SpoofedModel(
    val label: String,
    val deviceCode: Int,
    val isFake: Boolean = false
) {
    // ── Real Apple devices ──
    AIRPODS_GEN1(
        "AirPods (Gen 1)", 0x2002
    ),
    AIRPODS_GEN2(
        "AirPods (Gen 2)", 0x0F20
    ),
    AIRPODS_GEN3(
        "AirPods (Gen 3)", 0x1320
    ),
    AIRPODS_GEN4(
        "AirPods (Gen 4)", 0x1720
    ),
    AIRPODS_GEN4_ANC(
        "AirPods (Gen 4 ANC)", 0x1920
    ),
    AIRPODS_PRO(
        "AirPods Pro", 0x0E20
    ),
    AIRPODS_PRO2(
        "AirPods Pro 2", 0x1420
    ),
    AIRPODS_PRO2_USBC(
        "AirPods Pro 2 USB-C", 0x1620
    ),
    AIRPODS_PRO3(
        "AirPods Pro 3", 0x1B20
    ),
    AIRPODS_MAX(
        "AirPods Max", 0x0A20
    ),
    AIRPODS_MAX_USBC(
        "AirPods Max USB-C", 0x1A20
    ),
    BEATS_FIT_PRO(
        "Beats Fit Pro", 0x1220
    ),
    BEATS_STUDIO_BUDS(
        "Beats Studio Buds", 0x1120
    ),
    POWERBEATS_PRO(
        "Powerbeats Pro", 0x0B20
    ),
    POWERBEATS_PRO2(
        "Powerbeats Pro 2", 0x1820
    ),

    // ── Clone signatures ──
    // Clones spoof the same device code
    // but with non-standard message length.
    // Detection: same code + length ≠ 25
    FAKE_AIRPODS_GEN1(
        "AirPods (Gen 1) 🎭", 0x2002, true
    ),
    FAKE_AIRPODS_GEN2(
        "AirPods (Gen 2) 🎭", 0x0F20, true
    ),
    FAKE_AIRPODS_GEN3(
        "AirPods (Gen 3) 🎭", 0x1320, true
    ),
    FAKE_AIRPODS_PRO(
        "AirPods Pro 🎭", 0x0E20, true
    ),
    FAKE_AIRPODS_PRO2(
        "AirPods Pro 2 🎭", 0x1420, true
    ),

    UNKNOWN("Unknown Device", 0x0000);

    companion object {
        /**
         * Detects model from the 2-byte device code
         * in the proximity pairing payload.
         * If messageLength != 25, assume it's a clone.
         */
        fun detect(
            deviceCode: Int,
            messageLength: Int
        ): SpoofedModel {
            val isStandardLength = messageLength == 25

            // Try exact match first
            val real = entries.firstOrNull {
                !it.isFake &&
                    it != UNKNOWN &&
                    it.deviceCode == deviceCode
            }

            if (real != null) {
                // If non-standard length → fake
                if (!isStandardLength) {
                    val fake = entries.firstOrNull {
                        it.isFake &&
                            it.deviceCode == deviceCode
                    }
                    return fake ?: real
                }
                return real
            }
            return UNKNOWN
        }
    }
}

enum class CaseLidState {
    OPEN,
    CLOSED,
    NOT_IN_CASE,
    UNKNOWN
}

data class PodDeviceInfo(
    val deviceName: String = "",
    val macAddress: String = "",
    val spoofedModel: SpoofedModel =
        SpoofedModel.UNKNOWN,
    val rawDeviceCode: Int = 0,
    val rawDeviceCodeHex: String = "0x0000",
    val messageLength: Int = 0,
    val isFake: Boolean = false
)

enum class GestureAction(val displayName: String) {
    PLAY_PAUSE("Play / Pause"),
    FORWARD("Next Track"),
    BACKWARD("Previous Track"),
    NONE("No Effect"),
    DISCONNECT("Disconnects Pod")
}

data class GestureMapping(
    val gesture: String,
    val action: GestureAction
)

val DEFAULT_GESTURE_MAP = listOf(
    GestureMapping(
        "Single Tap",
        GestureAction.PLAY_PAUSE
    ),
    GestureMapping(
        "Swipe Up / Down",
        GestureAction.PLAY_PAUSE
    ),
    GestureMapping(
        "Double Tap Right",
        GestureAction.FORWARD
    ),
    GestureMapping(
        "Double Tap Left",
        GestureAction.BACKWARD
    ),
    GestureMapping(
        "Triple Tap",
        GestureAction.NONE
    ),
    GestureMapping(
        "Long Hold",
        GestureAction.DISCONNECT
    )
)

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTED
}

enum class MicrophoneLocation {
    LEFT,
    RIGHT,
    UNKNOWN
}

data class PodsUiState(
    val connectionState: ConnectionState =
        ConnectionState.DISCONNECTED,
    val battery: PodBattery = PodBattery(),
    val deviceInfo: PodDeviceInfo = PodDeviceInfo(),
    val gestures: List<GestureMapping> =
        DEFAULT_GESTURE_MAP,
    val showConnectionSheet: Boolean = false,
    val popupDismissedWhileDisconnected: Boolean =
        false,
    val caseLidState: CaseLidState =
        CaseLidState.UNKNOWN,
    val isCaseLidOpen: Boolean = false,
    val isLeftInEar: Boolean = false,
    val isRightInEar: Boolean = false,
    val isLeftInCase: Boolean = false,
    val isRightInCase: Boolean = false,
    val areBothPodsInCase: Boolean = false,
    val micLocation: MicrophoneLocation =
        MicrophoneLocation.UNKNOWN,
    val rssi: Int = 0
)
