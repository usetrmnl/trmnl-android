package ink.trmnl.android.data

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Data class containing useful HTTP metadata from the API response.
 * This information is extracted from okhttp3.Response and can be used for debugging and logging.
 */
@Keep
@JsonClass(generateAdapter = true)
data class HttpResponseMetadata(
    val url: String,
    val protocol: String,
    val statusCode: Int,
    val message: String,
    val contentType: String?,
    val contentLength: Long,
    val serverName: String?,
    val requestDuration: Long,
    val etag: String?,
    val requestId: String?,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        /**
         * Creates an empty metadata object with default values.
         * Used when no response metadata is available.
         */
        fun empty() =
            HttpResponseMetadata(
                url = "https://example.com",
                protocol = "http/1.1",
                statusCode = 0,
                message = "Not applicable",
                contentType = null,
                contentLength = -1,
                serverName = null,
                requestDuration = -1,
                etag = null,
                requestId = null,
                timestamp = System.currentTimeMillis(),
            )
    }
}
