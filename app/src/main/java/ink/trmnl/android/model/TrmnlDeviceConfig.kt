package ink.trmnl.android.model

import androidx.annotation.Keep
import ink.trmnl.android.data.AppConfig.TRMNL_API_SERVER_BASE_URL

/**
 * Represents the configuration for a TRMNL device.
 *
 * @see [TrmnlDeviceType]
 * @see [ink.trmnl.android.data.TrmnlDisplayInfo]
 */
@Keep
sealed interface TrmnlDeviceConfig {
    val type: TrmnlDeviceType

    /**
     * API access token for server with [apiBaseUrl].
     */
    val apiAccessToken: String

    /**
     * Provides API service base URL for the device based on device type.
     */
    val apiBaseUrl: String

    /**
     * TRMNL device image refresh rate in seconds.
     */
    val refreshRateSecs: Long

    data class TrmnlHardware(
        override val type: TrmnlDeviceType,
        override val apiAccessToken: String,
        override val apiBaseUrl: String = TRMNL_API_SERVER_BASE_URL,
        override val refreshRateSecs: Long,
    ) : TrmnlDeviceConfig

    data class ByodHardware(
        override val type: TrmnlDeviceType,
        override val apiAccessToken: String,
        override val apiBaseUrl: String = TRMNL_API_SERVER_BASE_URL,
        override val refreshRateSecs: Long,
    ) : TrmnlDeviceConfig

    data class ByosHardware(
        override val type: TrmnlDeviceType,
        override val apiAccessToken: String,
        override val apiBaseUrl: String,
        override val refreshRateSecs: Long,
    ) : TrmnlDeviceConfig
}
