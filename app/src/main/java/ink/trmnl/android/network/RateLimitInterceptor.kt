package ink.trmnl.android.network

import ink.trmnl.android.util.HTTP_429
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * - **Only retries idempotent methods (GET, HEAD)** to prevent duplicate operations
 * - Implements exponential backoff with jitter to avoid thundering herd
 * - Fully respects the Retry-After header if provided by the server (in seconds only)
 * - Retries the request up to a maximum number of attempts
 * - Logs retry attempts for debugging
 * - Emits retry events for UI feedback
 *
 * Exponential backoff formula (optimized for ~15s rate limit recovery):
 * - Base delay: 3 seconds (optimized for typical API rate limits)
 * - Delay = base * (2 ^ attempt-1) * jitter
 * - Jitter: delay * (0.5 + 0.5 * random) to distribute load
 * - Expected progression with jitter (for current ~15s rate limit):
 *   - Attempt 1: ~2-3s delay (total ~3s elapsed)
 *   - Attempt 2: ~4-6s delay (total ~7-9s elapsed)
 *   - Attempt 3: ~8-12s delay (total ~15-21s elapsed) - typically succeeds
 *   - Attempt 4: ~16-24s delay (total ~31-45s elapsed) - handles longer rate limits
 *   - Attempt 5: ~32s delay (total ~63-77s elapsed) - final safety net
 * - Max delay: 32 seconds per retry (for exponential backoff only)
 * - Retry-After header takes precedence and is NOT capped
 *
 * @param sleeper Injectable sleeper for delay execution (defaults to Thread.sleep).
 *                Allows fake implementations for testing without actual delays.
 *
 * See: https://github.com/usetrmnl/trmnl-android/issues/260
 */
class RateLimitInterceptor(
    private val sleeper: Sleeper = ThreadSleeper(),
) : Interceptor {
    /**
     * Event emitted when a retry attempt is about to happen.
     *
     * @param attempt Current retry attempt number (1-indexed)
     * @param maxRetries Maximum number of retries allowed
     * @param delayMs Delay in milliseconds before this retry
     * @param reason Reason for the retry ([REASON_EXPONENTIAL_BACKOFF] or [REASON_RETRY_AFTER_HEADER])
     */
    data class RetryEvent(
        val attempt: Int,
        val maxRetries: Int,
        val delayMs: Long,
        val reason: String,
    )

    private val _retryEvents = MutableSharedFlow<RetryEvent>(extraBufferCapacity = 10)

    /**
     * Flow that emits retry events for UI feedback.
     * UI can collect this to show retry progress to users.
     */
    val retryEvents: SharedFlow<RetryEvent> = _retryEvents.asSharedFlow()

    companion object {
        private const val TAG = "RateLimitInterceptor"

        /**
         * Retry reason constants for RetryEvent.
         */
        const val REASON_EXPONENTIAL_BACKOFF = "exponential_backoff"
        const val REASON_RETRY_AFTER_HEADER = "retry_after_header"

        /**
         * Maximum number of retry attempts for rate-limited requests.
         * After this many retries, the interceptor gives up and returns the 429 response.
         */
        private const val MAX_RETRIES = 5

        /**
         * Initial backoff delay in milliseconds (3 seconds).
         *
         * Optimized for typical API rate limit recovery time of ~15 seconds.
         * With exponential backoff and jitter, this produces:
         * - Attempt 1: ~2-3s delay (total ~3s elapsed)
         * - Attempt 2: ~4-6s delay (total ~7-9s elapsed)
         * - Attempt 3: ~8-12s delay (total ~15-21s elapsed) - typically succeeds at 15s
         * - Attempt 4: ~16-24s delay (total ~31-45s elapsed) - handles longer rate limits
         * - Attempt 5: ~32s delay (total ~63-77s elapsed) - final attempt
         *
         * This reduces wasted attempts compared to 1s initial backoff while still
         * providing enough attempts to handle future longer rate limit windows.
         */
        private const val INITIAL_BACKOFF_MS = 3000L

        /**
         * Maximum backoff delay in milliseconds (32 seconds).
         * Prevents exponential backoff from growing too large.
         */
        private const val MAX_BACKOFF_MS = 32_000L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0

        // Only retry idempotent methods (GET, HEAD) to prevent duplicate operations
        // POST, PUT, PATCH, DELETE should not be retried as they may cause side effects
        val isIdempotent = request.method == "GET" || request.method == "HEAD"

        // Retry loop for handling 429 responses (only for idempotent methods)
        while (response.code == HTTP_429 && attempt < MAX_RETRIES && isIdempotent) {
            attempt++

            // Calculate backoff delay
            val backoffDelay = calculateBackoffDelay(attempt, response)

            Timber.tag(TAG).w(
                "Rate limit exceeded (HTTP 429) for ${request.method} ${request.url}. " +
                    "Retry attempt $attempt/$MAX_RETRIES after ${backoffDelay}ms",
            )

            // Emit retry event for UI feedback
            val retryAfterHeader = response.header("Retry-After")
            val reason = if (retryAfterHeader != null) REASON_RETRY_AFTER_HEADER else REASON_EXPONENTIAL_BACKOFF
            _retryEvents.tryEmit(
                RetryEvent(
                    attempt = attempt,
                    maxRetries = MAX_RETRIES,
                    delayMs = backoffDelay,
                    reason = reason,
                ),
            )

            // Close the previous response before retrying
            response.close()

            // Wait for the backoff delay
            try {
                sleeper.sleep(backoffDelay)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted while waiting for rate limit backoff", e)
            }

            // Retry the request
            response = chain.proceed(request)
        }

        // Log if we exhausted all retries or hit non-idempotent method
        if (response.code == HTTP_429) {
            if (!isIdempotent) {
                Timber.tag(TAG).w(
                    "Rate limit exceeded (HTTP 429) for non-idempotent ${request.method} ${request.url}. " +
                        "Not retrying to prevent duplicate operations.",
                )
            } else if (attempt >= MAX_RETRIES) {
                Timber.tag(TAG).e(
                    "Rate limit exceeded (HTTP 429) for ${request.method} ${request.url}. " +
                        "Exhausted all $MAX_RETRIES retry attempts. Giving up.",
                )
            }
        }

        return response
    }

    /**
     * Calculates the backoff delay for the current retry attempt.
     *
     * Priority:
     * 1. Use Retry-After header if present (in seconds format only)
     *    - Retry-After values are NOT capped and are honored fully
     * 2. Use exponential backoff with jitter (capped at MAX_BACKOFF_MS)
     *
     * Note: HTTP-date format for Retry-After is not currently supported.
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
                // Honor the server's value without capping
                val delayMs = retryAfterSeconds * 1000
                Timber.tag(TAG).d("Using Retry-After header: ${retryAfterSeconds}s (${delayMs}ms)")
                return delayMs
            }
        }

        // Use exponential backoff with jitter
        // Formula: base * (2^attempt) * jitter
        // Jitter: random value between 0.5 and 1.0 to prevent thundering herd
        val exponentialDelay = INITIAL_BACKOFF_MS * (2.0.pow(attempt - 1)).toLong()
        val jitter = 0.5 + (0.5 * Random.nextDouble())
        val delayMs = (exponentialDelay * jitter).toLong()

        // Cap exponential backoff at maximum delay (but not Retry-After)
        val cappedDelayMs = min(delayMs, MAX_BACKOFF_MS)

        Timber.tag(TAG).d(
            "Using exponential backoff: attempt=$attempt, " +
                "exponential=${exponentialDelay}ms, jitter=${"%.2f".format(jitter)}, " +
                "delay=${delayMs}ms, capped=${cappedDelayMs}ms",
        )

        return cappedDelayMs
    }
}
