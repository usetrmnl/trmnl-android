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
     *
     * **API Usage:**
     * - Automatic refreshes use `/api/current_screen` endpoint (mirrors official TRMNL device)
     * - Does not advance playlist automatically
     * - Displays the same content as the official TRMNL hardware device
     */
    TRMNL,

    /**
     * Represents a Bring Your Own Device.
     * Build your own device with our modified firmware connecting to TRMNL servers.
     *
     * - Full access to TRMNL web app
     * - Plugin API access for DIY devices
     *
     * **API Usage (Configurable):**
     * - **Master mode (default)**: Auto-advances playlist using `/api/display` endpoint
     * - **Slave mode (optional)**: Mirrors another device using `/api/current_screen` endpoint
     * - Mode is controlled by the `isMasterDevice` setting in device configuration
     *
     * @see ink.trmnl.android.model.TrmnlDeviceConfig.isMasterDevice
     * @see <a href="https://github.com/usetrmnl/trmnl-android/issues/190">Issue #190</a>
     */
    BYOD,

    /**
     * Represents a Full DIY Stack - BYOD/S.
     * Bring your own device AND server.
     *
     * **API Usage:**
     * - Automatic refreshes use `/api/display` endpoint (advances through playlist)
     * - Automatically cycles through playlist items
     * - Independent content progression (not mirroring)
     */
    BYOS,
}
