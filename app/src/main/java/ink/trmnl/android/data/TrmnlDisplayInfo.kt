package ink.trmnl.android.data

import androidx.annotation.Keep
import ink.trmnl.android.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC
import ink.trmnl.android.model.TrmnlDeviceType

/**
 * Represents the display information for the TRMNL.
 * @see [TrmnlDisplayRepository]
 */
@Keep
data class TrmnlDisplayInfo constructor(
    val status: Int,
    val trmnlDeviceType: TrmnlDeviceType,
    val imageUrl: String,
    val imageName: String,
    val error: String? = null,
    val refreshIntervalSeconds: Long? = DEFAULT_REFRESH_INTERVAL_SEC,
)
