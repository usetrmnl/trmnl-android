package ink.trmnl.android.model

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import ink.trmnl.android.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC

/**
 * Represents the self-contained configuration for a TRMNL device of [TrmnlDeviceType].
 *
 * NOTE: This will help us add support for multiple devices in the future.
 *       See https://github.com/usetrmnl/trmnl-android/issues/41
 *
 * @see [TrmnlDeviceType]
 * @see [ink.trmnl.android.data.TrmnlDisplayInfo]
 */
@Keep
@JsonClass(generateAdapter = true)
data class TrmnlDeviceConfig constructor(
    val type: TrmnlDeviceType,
    val apiBaseUrl: String,
    val apiAccessToken: String,
    /**
     * Device MAC address for API authentication, usually used by BYOS servers.
     */
    val deviceMacId: String? = null,
    val refreshRateSecs: Long = DEFAULT_REFRESH_INTERVAL_SEC,
    /**
     * Determines if BYOD device acts as master (auto-advance playlist) or slave (mirror).
     * - `true`: Device auto-advances playlist (uses /api/display endpoint)
     * - `false`: Device mirrors another TRMNL device (uses /api/current_screen endpoint)
     * - `null`: Not applicable (for TRMNL and BYOS devices) or backward compatibility (defaults to true for BYOD)
     *
     * See https://github.com/usetrmnl/trmnl-android/issues/190
     */
    val isMasterDevice: Boolean? = null,
    /**
     * User-level API token (Account API key) for user-level endpoints.
     *
     * **DEPRECATED:** This field is no longer needed for battery reporting.
     * Battery percentage is now sent via the Percent-Charged header in /api/display call,
     * which only requires device-level authentication ([apiAccessToken]).
     *
     * This field is kept for backward compatibility and may be removed in a future version.
     *
     * Required for BYOD devices to access user-level API endpoints like /api/me and /api/devices.
     *
     * This is separate from [apiAccessToken] which is the device-level API key.
     */
    @Deprecated(
        message = "No longer needed for battery reporting. Battery is now sent via Percent-Charged header.",
        level = DeprecationLevel.WARNING,
    )
    val userApiToken: String? = null,
    /**
     * TRMNL device ID extracted from /api/devices/me endpoint.
     *
     * **DEPRECATED:** This field is no longer needed for battery reporting.
     * Battery percentage is now sent via the Percent-Charged header in /api/display call,
     * which doesn't require device ID or user-level authentication.
     *
     * This field is kept for backward compatibility and may be removed in a future version.
     *
     * Used for making user-level API calls to /api/devices/{id}.
     *
     * This ID is fetched during BYOD device validation and is required for
     * reporting battery status and other device-specific updates.
     *
     * **Note:** This field is only applicable for BYOD device types.
     */
    @Deprecated(
        message = "No longer needed for battery reporting. Battery is now sent via Percent-Charged header.",
        level = DeprecationLevel.WARNING,
    )
    val deviceId: Int? = null,
)
