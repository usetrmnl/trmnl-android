package ink.trmnl.android.data

/**
 * Represents the setup information for a TRMNL device.
 *
 * This data class is used to encapsulate the result of setting up a new device,
 * including whether the setup was successful, the device's MAC ID, API key,
 * and any relevant messages.
 */
data class DeviceSetupInfo(
    val success: Boolean,
    val deviceMacId: String,
    val apiKey: String,
    val message: String,
)
