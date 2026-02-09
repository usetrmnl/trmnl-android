package ink.trmnl.android.network

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * OkHttp interceptor that handles HTTP 429 (Too Many Requests) responses with exponential backoff.
 *
 * This interceptor:
 * - Detects HTTP 429 rate limit responses
 * - Implements exponential backoff with jitter to avoid thundering herd
 * - Respects the Retry-After header if provided by the server
 * - Retries the request up to a maximum number of attempts
 * - Logs retry attempts for debugging
 *
 * Exponential backoff formula:
 * - Base delay: 1 second
 * - Delay = base * (2 ^ attempt) with jitter
 * - Jitter: delay * (0.5 + 0.5 * random) to distribute load
 * - Max delay: 32 seconds per retry
 *
 * See: https://github.com/usetrmnl/trmnl-android/issues/260
 */
class RateLimitInterceptor : Interceptor {
    companion object {
        private const val TAG = "RateLimitInterceptor"

        /**
         * Maximum number of retry attempts for rate-limited requests.
         * After this many retries, the interceptor gives up and returns the 429 response.
         */
        private const val MAX_RETRIES = 5

        /**
         * Initial backoff delay in milliseconds (1 second).
         */
        private const val INITIAL_BACKOFF_MS = 1000L

        /**
         * Maximum backoff delay in milliseconds (32 seconds).
         * Prevents exponential backoff from growing too large.
         */
        private const val MAX_BACKOFF_MS = 32_000L

        /**
         * HTTP status code for "Too Many Requests" (rate limiting).
         */
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0

        // Retry loop for handling 429 responses
        while (response.code == HTTP_TOO_MANY_REQUESTS && attempt < MAX_RETRIES) {
            attempt++

            // Calculate backoff delay
            val backoffDelay = calculateBackoffDelay(attempt, response)

            Timber.tag(TAG).w(
                "Rate limit exceeded (HTTP 429) for ${request.url}. " +
                    "Retry attempt $attempt/$MAX_RETRIES after ${backoffDelay}ms",
            )

            // Close the previous response before retrying
            response.close()

            // Wait for the backoff delay
            try {
                Thread.sleep(backoffDelay)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted while waiting for rate limit backoff", e)
            }

            // Retry the request
            response = chain.proceed(request)
        }

        // Log if we exhausted all retries
        if (response.code == HTTP_TOO_MANY_REQUESTS && attempt >= MAX_RETRIES) {
            Timber.tag(TAG).e(
                "Rate limit exceeded (HTTP 429) for ${request.url}. " +
                    "Exhausted all $MAX_RETRIES retry attempts. Giving up.",
            )
        }

        return response
    }

    /**
     * Calculates the backoff delay for the current retry attempt.
     *
     * Priority:
     * 1. Use Retry-After header if present (seconds or HTTP-date)
     * 2. Use exponential backoff with jitter
     *
     * @param attempt Current retry attempt (1-indexed)
     * @param response The 429 response containing potential Retry-After header
     * @return Backoff delay in milliseconds
     */
    private fun calculateBackoffDelay(
        attempt: Int,
        response: Response,
    ): Long {
        // Check for Retry-After header (RFC 7231)
        val retryAfterHeader = response.header("Retry-After")
        if (retryAfterHeader != null) {
            val retryAfterSeconds = retryAfterHeader.toLongOrNull()
            if (retryAfterSeconds != null) {
                // Retry-After is in seconds, convert to milliseconds
                val delayMs = retryAfterSeconds * 1000
                Timber.tag(TAG).d("Using Retry-After header: ${retryAfterSeconds}s (${delayMs}ms)")
                return min(delayMs, MAX_BACKOFF_MS)
            }
            // Note: We don't handle HTTP-date format for Retry-After as it's rarely used
            // If needed, it can be parsed using SimpleDateFormat or java.time APIs
        }

        // Use exponential backoff with jitter
        // Formula: base * (2^attempt) * jitter
        // Jitter: random value between 0.5 and 1.0 to prevent thundering herd
        val exponentialDelay = INITIAL_BACKOFF_MS * (2.0.pow(attempt - 1)).toLong()
        val jitter = 0.5 + (0.5 * Random.nextDouble())
        val delayMs = (exponentialDelay * jitter).toLong()

        // Cap at maximum backoff delay
        val cappedDelayMs = min(delayMs, MAX_BACKOFF_MS)

        Timber.tag(TAG).d(
            "Using exponential backoff: attempt=$attempt, " +
                "exponential=${exponentialDelay}ms, jitter=${"%.2f".format(jitter)}, " +
                "delay=${delayMs}ms, capped=${cappedDelayMs}ms",
        )

        return cappedDelayMs
    }
}
