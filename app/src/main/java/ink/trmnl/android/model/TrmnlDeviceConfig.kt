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
    val refreshRateSecs: Long = DEFAULT_REFRESH_INTERVAL_SEC,
)
