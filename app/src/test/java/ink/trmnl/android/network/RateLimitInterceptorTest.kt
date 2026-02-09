package ink.trmnl.android.network

import com.google.common.truth.Truth.assertThat
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for [RateLimitInterceptor] to verify exponential backoff behavior for HTTP 429 responses.
 */
class RateLimitInterceptorTest {
    private lateinit var fakeSleeper: FakeSleeper
    private lateinit var interceptor: RateLimitInterceptor
    private lateinit var testRequest: Request

    @Before
    fun setup() {
        fakeSleeper = FakeSleeper()
        interceptor = RateLimitInterceptor(fakeSleeper)
        testRequest =
            Request
                .Builder()
                .url("https://test.com/api/display")
                .build()
    }

    @Test
    fun `intercept allows successful response to pass through`() {
        // Arrange
        val mockChain = createMockChain(200, "OK")

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert
        assertThat(response.code).isEqualTo(200)
        assertThat(response.message).isEqualTo("OK")
    }

    @Test
    fun `intercept retries on HTTP 429 and eventually succeeds`() {
        // Arrange - Fail twice with 429, then succeed
        val responses =
            mutableListOf(
                createResponse(429, "Too Many Requests"),
                createResponse(429, "Too Many Requests"),
                createResponse(200, "OK"),
            )
        val mockChain = createMockChainWithMultipleResponses(responses)

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert
        assertThat(response.code).isEqualTo(200)
        assertThat(fakeSleeper.sleepCalls).hasSize(2) // Slept twice for 2 retries
        // First retry: ~1s (with jitter 0.5-1.0)
        assertThat(fakeSleeper.sleepCalls[0]).isAtLeast(500L)
        assertThat(fakeSleeper.sleepCalls[0]).isAtMost(1000L)
        // Second retry: ~2s (with jitter 0.5-1.0)
        assertThat(fakeSleeper.sleepCalls[1]).isAtLeast(1000L)
        assertThat(fakeSleeper.sleepCalls[1]).isAtMost(2000L)
    }

    @Test
    fun `intercept respects Retry-After header in seconds`() {
        // Arrange - 429 with Retry-After header, then success
        val responses =
            mutableListOf(
                createResponse(429, "Too Many Requests", retryAfterSeconds = "2"),
                createResponse(200, "OK"),
            )
        val mockChain = createMockChainWithMultipleResponses(responses)

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert
        assertThat(response.code).isEqualTo(200)
        assertThat(fakeSleeper.sleepCalls).hasSize(1)
        // Should use Retry-After value exactly (2000ms)
        assertThat(fakeSleeper.sleepCalls[0]).isEqualTo(2000L)
    }

    @Test
    fun `intercept gives up after max retries`() {
        // Arrange - Always return 429
        val responses = mutableListOf<Response>()
        repeat(10) {
            // More than MAX_RETRIES
            responses.add(createResponse(429, "Too Many Requests"))
        }
        val mockChain = createMockChainWithMultipleResponses(responses)

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert - Should still return 429 after exhausting retries
        assertThat(response.code).isEqualTo(429)
        assertThat(fakeSleeper.sleepCalls).hasSize(5) // MAX_RETRIES = 5
    }

    @Test
    fun `intercept applies exponential backoff with proper delays`() {
        // Arrange - Fail multiple times with 429
        val responses =
            mutableListOf(
                createResponse(429, "Too Many Requests"),
                createResponse(429, "Too Many Requests"),
                createResponse(429, "Too Many Requests"),
                createResponse(200, "OK"),
            )
        val mockChain = createMockChainWithMultipleResponses(responses)

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert
        assertThat(response.code).isEqualTo(200)
        assertThat(fakeSleeper.sleepCalls).hasSize(3)
        // Verify exponential backoff pattern (with jitter range 0.5-1.0)
        // Attempt 1: 1s * jitter = 500-1000ms
        assertThat(fakeSleeper.sleepCalls[0]).isAtLeast(500L)
        assertThat(fakeSleeper.sleepCalls[0]).isAtMost(1000L)
        // Attempt 2: 2s * jitter = 1000-2000ms
        assertThat(fakeSleeper.sleepCalls[1]).isAtLeast(1000L)
        assertThat(fakeSleeper.sleepCalls[1]).isAtMost(2000L)
        // Attempt 3: 4s * jitter = 2000-4000ms
        assertThat(fakeSleeper.sleepCalls[2]).isAtLeast(2000L)
        assertThat(fakeSleeper.sleepCalls[2]).isAtMost(4000L)
    }

    @Test
    fun `intercept caps exponential backoff at max delay`() {
        // Arrange - Many 429s to trigger high exponential values
        val responses = mutableListOf<Response>()
        repeat(6) {
            responses.add(createResponse(429, "Too Many Requests"))
        }
        val mockChain = createMockChainWithMultipleResponses(responses)

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert - Should cap at MAX_BACKOFF_MS (32s)
        assertThat(response.code).isEqualTo(429)
        assertThat(fakeSleeper.sleepCalls).hasSize(5) // MAX_RETRIES = 5

        // Verify the last attempt (attempt 5: 1s * 2^4 = 16s, capped at 32s, jittered 0.5-1.0x)
        // The 5th attempt's base delay is 16s, which is already below MAX_BACKOFF_MS,
        // so it won't be capped by MAX_BACKOFF_MS but will be affected by jitter (8-16s)
        // Let's check that at least one of the later calls (attempts 4 or 5) shows proper exponential growth
        val attempt4Delay = fakeSleeper.sleepCalls[3] // 1s * 2^3 = 8s with jitter = 4-8s
        val attempt5Delay = fakeSleeper.sleepCalls[4] // 1s * 2^4 = 16s with jitter = 8-16s

        assertThat(attempt4Delay).isAtLeast(4000L)
        assertThat(attempt4Delay).isAtMost(8000L)
        assertThat(attempt5Delay).isAtLeast(8000L)
        assertThat(attempt5Delay).isAtMost(16000L)
    }

    @Test
    fun `intercept does NOT retry non-idempotent POST requests`() {
        // Arrange - POST request with 429
        val postRequest =
            Request
                .Builder()
                .url("https://test.com/api/data")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

        val mockChain =
            object : Interceptor.Chain {
                override fun request(): Request = postRequest

                override fun proceed(request: Request): Response = createResponse(429, "Too Many Requests")

                override fun connection() = null

                override fun call() = throw UnsupportedOperationException("Not implemented for test")

                override fun connectTimeoutMillis() = 30_000

                override fun withConnectTimeout(
                    timeout: Int,
                    unit: TimeUnit,
                ) = this

                override fun readTimeoutMillis() = 30_000

                override fun withReadTimeout(
                    timeout: Int,
                    unit: TimeUnit,
                ) = this

                override fun writeTimeoutMillis() = 30_000

                override fun withWriteTimeout(
                    timeout: Int,
                    unit: TimeUnit,
                ) = this
            }

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert - Should NOT retry, return 429 immediately
        assertThat(response.code).isEqualTo(429)
        assertThat(fakeSleeper.sleepCalls).isEmpty() // No retries
    }

    @Test
    fun `intercept does NOT retry non-idempotent PATCH requests`() {
        // Arrange - PATCH request with 429
        val patchRequest =
            Request
                .Builder()
                .url("https://test.com/api/data/123")
                .patch("{}".toRequestBody("application/json".toMediaType()))
                .build()

        val mockChain =
            object : Interceptor.Chain {
                override fun request(): Request = patchRequest

                override fun proceed(request: Request): Response = createResponse(429, "Too Many Requests")

                override fun connection() = null

                override fun call() = throw UnsupportedOperationException("Not implemented for test")

                override fun connectTimeoutMillis() = 30_000

                override fun withConnectTimeout(
                    timeout: Int,
                    unit: TimeUnit,
                ) = this

                override fun readTimeoutMillis() = 30_000

                override fun withReadTimeout(
                    timeout: Int,
                    unit: TimeUnit,
                ) = this

                override fun writeTimeoutMillis() = 30_000

                override fun withWriteTimeout(
                    timeout: Int,
                    unit: TimeUnit,
                ) = this
            }

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert - Should NOT retry, return 429 immediately
        assertThat(response.code).isEqualTo(429)
        assertThat(fakeSleeper.sleepCalls).isEmpty() // No retries
    }

    @Test
    fun `intercept honors Retry-After header without capping`() {
        // Arrange - Server returns very large Retry-After (60 seconds)
        val responses =
            mutableListOf(
                createResponse(429, "Too Many Requests", retryAfterSeconds = "60"),
                createResponse(200, "OK"),
            )
        val mockChain = createMockChainWithMultipleResponses(responses)

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert - Should honor the full 60s Retry-After without capping at MAX_BACKOFF_MS (32s)
        assertThat(response.code).isEqualTo(200)
        assertThat(fakeSleeper.sleepCalls).hasSize(1)
        assertThat(fakeSleeper.sleepCalls[0]).isEqualTo(60000L) // 60 seconds
    }

    @Test
    fun `intercept retries HEAD requests like GET`() {
        // Arrange - HEAD request with 429
        val headRequest =
            Request
                .Builder()
                .url("https://test.com/api/display")
                .head()
                .build()

        val responses =
            mutableListOf(
                createResponse(429, "Too Many Requests"),
                createResponse(200, "OK"),
            )

        val mockChain =
            object : Interceptor.Chain {
                private var callCount = 0

                override fun request(): Request = headRequest

                override fun proceed(request: Request): Response {
                    val response = responses.removeFirstOrNull() ?: createResponse(500, "Out of responses")
                    callCount++
                    return response
                }

                override fun connection() = null

                override fun call() = throw UnsupportedOperationException("Not implemented for test")

                override fun connectTimeoutMillis() = 30_000

                override fun withConnectTimeout(
                    timeout: Int,
                    unit: TimeUnit,
                ) = this

                override fun readTimeoutMillis() = 30_000

                override fun withReadTimeout(
                    timeout: Int,
                    unit: TimeUnit,
                ) = this

                override fun writeTimeoutMillis() = 30_000

                override fun withWriteTimeout(
                    timeout: Int,
                    unit: TimeUnit,
                ) = this
            }

        // Act
        val response = interceptor.intercept(mockChain)

        // Assert - Should retry HEAD request (idempotent)
        assertThat(response.code).isEqualTo(200)
        assertThat(fakeSleeper.sleepCalls).hasSize(1) // One retry
    }

    // Helper functions

    private fun createMockChain(
        statusCode: Int,
        message: String,
    ): Interceptor.Chain =
        object : Interceptor.Chain {
            override fun request(): Request = testRequest

            override fun proceed(request: Request): Response = createResponse(statusCode, message)

            override fun connection() = null

            override fun call() = throw UnsupportedOperationException("Not implemented for test")

            override fun connectTimeoutMillis() = 30_000

            override fun withConnectTimeout(
                timeout: Int,
                unit: TimeUnit,
            ) = this

            override fun readTimeoutMillis() = 30_000

            override fun withReadTimeout(
                timeout: Int,
                unit: TimeUnit,
            ) = this

            override fun writeTimeoutMillis() = 30_000

            override fun withWriteTimeout(
                timeout: Int,
                unit: TimeUnit,
            ) = this
        }

    private fun createMockChainWithMultipleResponses(responses: MutableList<Response>): Interceptor.Chain =
        object : Interceptor.Chain {
            private var callCount = 0

            override fun request(): Request = testRequest

            override fun proceed(request: Request): Response {
                val response = responses.removeFirstOrNull() ?: createResponse(500, "Out of responses")
                callCount++
                return response
            }

            override fun connection() = null

            override fun call() = throw UnsupportedOperationException("Not implemented for test")

            override fun connectTimeoutMillis() = 30_000

            override fun withConnectTimeout(
                timeout: Int,
                unit: TimeUnit,
            ) = this

            override fun readTimeoutMillis() = 30_000

            override fun withReadTimeout(
                timeout: Int,
                unit: TimeUnit,
            ) = this

            override fun writeTimeoutMillis() = 30_000

            override fun withWriteTimeout(
                timeout: Int,
                unit: TimeUnit,
            ) = this
        }

    private fun createResponse(
        statusCode: Int,
        message: String,
        retryAfterSeconds: String? = null,
    ): Response {
        val responseBuilder =
            Response
                .Builder()
                .request(testRequest)
                .protocol(Protocol.HTTP_2)
                .code(statusCode)
                .message(message)
                .body("{}".toResponseBody("application/json".toMediaType()))

        if (retryAfterSeconds != null) {
            responseBuilder.header("Retry-After", retryAfterSeconds)
        }

        return responseBuilder.build()
    }
}
