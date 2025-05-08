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
    val apiBaseUrl: String
        get() =
            when (this) {
                is TrmnlHardware -> TRMNL_API_SERVER_BASE_URL
                is ByodHardware -> TRMNL_API_SERVER_BASE_URL
                is ByosHardware -> serviceBaseUrl
            }

    data class TrmnlHardware(
        override val type: TrmnlDeviceType,
    ) : TrmnlDeviceConfig

    data class ByodHardware(
        override val type: TrmnlDeviceType,
    ) : TrmnlDeviceConfig

    data class ByosHardware(
        override val type: TrmnlDeviceType,
        val serviceBaseUrl: String,
    ) : TrmnlDeviceConfig
}
