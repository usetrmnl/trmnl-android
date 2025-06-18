package ink.trmnl.android.network.util

import com.slack.eithernet.ApiResult
import com.slack.eithernet.InternalEitherNetApi
import ink.trmnl.android.data.HttpResponseMetadata

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
