package ink.trmnl.android.data

import androidx.annotation.Keep
import ink.trmnl.android.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.util.HTTP_200
import ink.trmnl.android.util.HTTP_500
import ink.trmnl.android.util.HTTP_OK

/**
 * Represents the display information for the TRMNL.
 * @see [TrmnlDisplayRepository]
 */
@Keep
data class TrmnlDisplayInfo constructor(
    /**
     * See [HTTP_OK], [HTTP_200], [HTTP_500].
     */
    val status: Int,
    val trmnlDeviceType: TrmnlDeviceType,
    val imageUrl: String,
    val imageName: String,
    val error: String? = null,
    val refreshIntervalSeconds: Long? = DEFAULT_REFRESH_INTERVAL_SEC,
)
