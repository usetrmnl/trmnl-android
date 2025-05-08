package ink.trmnl.android.model

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Represents the configuration for a TRMNL device.
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
    val refreshRateSecs: Long,
)
