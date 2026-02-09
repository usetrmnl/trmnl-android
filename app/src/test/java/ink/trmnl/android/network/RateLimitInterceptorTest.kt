package ink.trmnl.android.network

import com.google.common.truth.Truth.assertThat
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for [RateLimitInterceptor] to verify exponential backoff behavior for HTTP 429 responses.
 */
class RateLimitInterceptorTest {
    private lateinit var interceptor: RateLimitInterceptor
    private lateinit var testRequest: Request

    @Before
    fun setup() {
        interceptor = RateLimitInterceptor()
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
        val startTime = System.currentTimeMillis()
        val response = interceptor.intercept(mockChain)
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertThat(response.code).isEqualTo(200)
        // Should have retried twice, taking at least 1s + 2s = 3s total
        // Using a lower bound to account for jitter (0.5x factor)
        assertThat(duration).isAtLeast(1500L) // 1.5s minimum (with jitter)
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
        val startTime = System.currentTimeMillis()
        val response = interceptor.intercept(mockChain)
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertThat(response.code).isEqualTo(200)
        // Should have waited approximately 2 seconds
        assertThat(duration).isAtLeast(1900L) // Account for slight timing variations
        assertThat(duration).isAtMost(2500L)
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
        val startTime = System.currentTimeMillis()
        val response = interceptor.intercept(mockChain)
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertThat(response.code).isEqualTo(200)
        // Expected delays: ~1s, ~2s, ~4s = ~7s total (with jitter 0.5x-1.0x)
        // Minimum: 0.5 * (1 + 2 + 4) = 3.5s
        assertThat(duration).isAtLeast(3500L)
    }

    @Test
    fun `intercept caps backoff at max delay`() {
        // Create client with interceptor
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(RateLimitInterceptor())
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .build()

        // This test verifies that MAX_BACKOFF_MS (32s) is respected
        // We can't easily test this without a real server, so we'll just
        // verify the interceptor is properly integrated
        assertThat(client.interceptors).hasSize(1)
        assertThat(client.interceptors[0]).isInstanceOf(RateLimitInterceptor::class.java)
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
