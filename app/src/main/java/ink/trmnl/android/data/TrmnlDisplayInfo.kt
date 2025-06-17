package ink.trmnl.android.data

import androidx.annotation.Keep
import ink.trmnl.android.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.util.ERROR_TYPE_DEVICE_SETUP_REQUIRED
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
    /**
     * The file name of the image to be displayed.
     *
     * If this is an error type, it indicates a specific error condition.
     * For example:
     * - [ERROR_TYPE_DEVICE_SETUP_REQUIRED]
     */
    val imageFileName: String,
    val error: String? = null,
    val refreshIntervalSeconds: Long? = DEFAULT_REFRESH_INTERVAL_SEC,
    /**
     * Metadata about the HTTP response that returned this display info.
     * Contains useful information for debugging and logging.
     */
    val httpResponseMetadata: HttpResponseMetadata? = null,
)
