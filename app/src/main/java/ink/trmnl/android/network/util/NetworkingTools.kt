package ink.trmnl.android.network.util

import com.slack.eithernet.ApiResult
import com.slack.eithernet.InternalEitherNetApi
import ink.trmnl.android.data.HttpResponseMetadata

/**
 * Constructs the full URL for API requests based on the configured base URL for device.
 */
internal fun constructApiUrl(
    baseUrl: String,
    endpoint: String,
): String =
    if (baseUrl.endsWith("/")) {
        "${baseUrl}$endpoint"
    } else {
        "$baseUrl/$endpoint"
    }

/**
 * Extracts HTTP response metadata from an ApiResult.Success instance.
 * The metadata is retrieved from the okhttp3.Response object stored in the ApiResult's tags.
 *
 * @param apiResult The successful API result containing response metadata
 * @return HttpResponseMetadata object containing useful response information, or null if not available
 */
@OptIn(InternalEitherNetApi::class)
internal fun extractHttpResponseMetadata(apiResult: ApiResult.Success<*>): HttpResponseMetadata? {
    val httpResponse = apiResult.tags[okhttp3.Response::class] as? okhttp3.Response ?: return null

    // Calculate the request duration using the timestamps from the response
    val requestDuration = httpResponse.receivedResponseAtMillis - httpResponse.sentRequestAtMillis

    return HttpResponseMetadata(
        url = httpResponse.request.url.toString(),
        protocol = httpResponse.protocol.toString(),
        statusCode = httpResponse.code,
        message = httpResponse.message,
        contentType = httpResponse.header("Content-Type"),
        contentLength = httpResponse.header("Content-Length")?.toLongOrNull() ?: -1,
        serverName = httpResponse.header("Server"),
        requestDuration = requestDuration,
        etag = httpResponse.header("etag"),
        requestId = httpResponse.header("x-request-id"),
        timestamp = System.currentTimeMillis(),
    )
}

/**
 * Extracts HTTP response metadata from an ApiResult.Failure instance.
 * For HttpFailure cases, constructs metadata from available information.
 * For other failure types, returns null.
 *
 * @param apiResult The failed API result
 * @param requestUrl The URL that was requested
 * @return HttpResponseMetadata object containing useful response information, or null if not available
 */
internal fun extractHttpResponseMetadataFromFailure(
    apiResult: ApiResult.Failure<*>,
    requestUrl: String,
): HttpResponseMetadata? =
    when (apiResult) {
        is ApiResult.Failure.HttpFailure -> {
            // HttpFailure contains the HTTP status code and error
            // Construct basic metadata from available information
            HttpResponseMetadata(
                url = requestUrl,
                protocol = "http/1.1", // Default, as we don't have access to actual protocol
                statusCode = apiResult.code,
                message = apiResult.error?.toString() ?: "",
                contentType = null,
                contentLength = -1,
                serverName = null,
                requestDuration = -1,
                etag = null,
                requestId = null,
                timestamp = System.currentTimeMillis(),
            )
        }
        else -> null // NetworkFailure, ApiFailure, and UnknownFailure don't have HTTP response data
    }
