package ink.trmnl.android.data.log

import com.google.common.truth.Truth.assertThat
import ink.trmnl.android.data.HttpResponseMetadata
import ink.trmnl.android.model.TrmnlDeviceType
import org.junit.Test

/**
 * Unit tests for TrmnlRefreshLog data class and its factory methods.
 */
class TrmnlRefreshLogTest {
    @Test
    fun `createSuccess creates log with correct values and success flag true`() {
        val result =
            TrmnlRefreshLog.createSuccess(
                trmnlDeviceType = TrmnlDeviceType.TRMNL,
                imageUrl = "https://test.com/image.png",
                imageName = "test-image.png",
                refreshIntervalSeconds = 600L,
                imageRefreshWorkType = "PERIODIC",
            )

        assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)
        assertThat(result.imageUrl).isEqualTo("https://test.com/image.png")
        assertThat(result.imageName).isEqualTo("test-image.png")
        assertThat(result.refreshIntervalSeconds).isEqualTo(600L)
        assertThat(result.imageRefreshWorkType).isEqualTo("PERIODIC")
        assertThat(result.success).isTrue()
        assertThat(result.error).isNull()
        assertThat(result.timestamp).isGreaterThan(0L)
    }

    @Test
    fun `createSuccess creates log with httpResponseMetadata when provided`() {
        val httpMetadata =
            HttpResponseMetadata(
                url = "https://test.com/api/display",
                protocol = "h2",
                statusCode = 200,
                message = "OK",
                contentType = "application/json",
                contentLength = 1234L,
                serverName = "TestServer",
                requestDuration = 500L,
                etag = "W/\"abc-123\"",
                requestId = "req-123",
            )

        val result =
            TrmnlRefreshLog.createSuccess(
                trmnlDeviceType = TrmnlDeviceType.BYOS,
                imageUrl = "https://test.com/image.png",
                imageName = "test-image.png",
                refreshIntervalSeconds = 300L,
                imageRefreshWorkType = "ONE_TIME",
                httpResponseMetadata = httpMetadata,
            )

        assertThat(result.httpResponseMetadata).isEqualTo(httpMetadata)
        assertThat(result.success).isTrue()
    }

    @Test
    fun `createSuccess creates log with null refreshIntervalSeconds`() {
        val result =
            TrmnlRefreshLog.createSuccess(
                trmnlDeviceType = TrmnlDeviceType.BYOD,
                imageUrl = "https://test.com/image.png",
                imageName = "test-image.png",
                refreshIntervalSeconds = null,
                imageRefreshWorkType = null,
            )

        assertThat(result.refreshIntervalSeconds).isNull()
        assertThat(result.imageRefreshWorkType).isNull()
        assertThat(result.success).isTrue()
    }

    @Test
    fun `createFailure creates log with correct values and success flag false`() {
        val result =
            TrmnlRefreshLog.createFailure(
                error = "Device not found",
            )

        assertThat(result.error).isEqualTo("Device not found")
        assertThat(result.success).isFalse()
        assertThat(result.imageUrl).isNull()
        assertThat(result.imageName).isNull()
        assertThat(result.refreshIntervalSeconds).isNull()
        assertThat(result.imageRefreshWorkType).isNull()
        assertThat(result.timestamp).isGreaterThan(0L)
        // Device type is set to TRMNL as default for failures
        assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)
    }

    @Test
    fun `createFailure creates log with httpResponseMetadata when provided`() {
        val httpMetadata =
            HttpResponseMetadata(
                url = "https://test.com/api/display",
                protocol = "h2",
                statusCode = 429,
                message = "Too Many Requests",
                contentType = "text/html",
                contentLength = -1L,
                serverName = "cloudflare",
                requestDuration = 50L,
                etag = null,
                requestId = "req-456",
            )

        val result =
            TrmnlRefreshLog.createFailure(
                error = "Rate limit exceeded",
                httpResponseMetadata = httpMetadata,
            )

        assertThat(result.httpResponseMetadata).isEqualTo(httpMetadata)
        assertThat(result.error).isEqualTo("Rate limit exceeded")
        assertThat(result.success).isFalse()
    }

    @Test
    fun `createFailure creates log without httpResponseMetadata when not provided`() {
        val result =
            TrmnlRefreshLog.createFailure(
                error = "Network error",
            )

        assertThat(result.httpResponseMetadata).isNull()
        assertThat(result.error).isEqualTo("Network error")
    }

    @Test
    fun `timestamp is set to current time for both success and failure logs`() {
        val beforeTimestamp = System.currentTimeMillis()

        val successLog =
            TrmnlRefreshLog.createSuccess(
                trmnlDeviceType = TrmnlDeviceType.TRMNL,
                imageUrl = "https://test.com/image.png",
                imageName = "test-image.png",
                refreshIntervalSeconds = 600L,
                imageRefreshWorkType = "PERIODIC",
            )

        val failureLog =
            TrmnlRefreshLog.createFailure(
                error = "Test error",
            )

        val afterTimestamp = System.currentTimeMillis()

        assertThat(successLog.timestamp).isAtLeast(beforeTimestamp)
        assertThat(successLog.timestamp).isAtMost(afterTimestamp)
        assertThat(failureLog.timestamp).isAtLeast(beforeTimestamp)
        assertThat(failureLog.timestamp).isAtMost(afterTimestamp)
    }
}
