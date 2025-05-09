package ink.trmnl.android.model

import androidx.annotation.Keep

/**
 * Enum class to represent the type of TRMNL device and service.
 * - https://usetrmnl.com/developers
 */
@Keep
enum class TrmnlDeviceType {
    /**
     * Represents a TRMNL device.
     */
    TRMNL,

    /**
     * Represents a Bring Your Own Device.
     * Build your own device with our modified firmware connecting to TRMNL servers.
     *
     * - Full access to TRMNL web app
     * - Plugin API access for DIY devices
     */
    BYOD,

    /**
     * Represents a Full DIY Stack - BYOD/S.
     * Bring your own device AND server.
     */
    BYOS,
}
