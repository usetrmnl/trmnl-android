package ink.trmnl.android.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for HttpResponseMetadata data class and its companion object methods.
 */
class HttpResponseMetadataTest {
    @Test
    fun `empty creates metadata with default values`() {
        val result = HttpResponseMetadata.empty()

        assertThat(result.url).isEqualTo("https://example.com")
        assertThat(result.protocol).isEqualTo("http/1.1")
        assertThat(result.statusCode).isEqualTo(0)
        assertThat(result.message).isEqualTo("Not applicable")
        assertThat(result.contentType).isNull()
        assertThat(result.contentLength).isEqualTo(-1)
        assertThat(result.serverName).isNull()
        assertThat(result.requestDuration).isEqualTo(-1)
        assertThat(result.etag).isNull()
        assertThat(result.requestId).isNull()
        assertThat(result.timestamp).isGreaterThan(0L)
    }

    @Test
    fun `empty creates metadata with current timestamp`() {
        val beforeTimestamp = System.currentTimeMillis()
        val result = HttpResponseMetadata.empty()
        val afterTimestamp = System.currentTimeMillis()

        assertThat(result.timestamp).isAtLeast(beforeTimestamp)
        assertThat(result.timestamp).isAtMost(afterTimestamp)
    }

    @Test
    fun `constructor creates metadata with all provided values`() {
        val result =
            HttpResponseMetadata(
                url = "https://api.example.com/test",
                protocol = "h2",
                statusCode = 200,
                message = "OK",
                contentType = "application/json",
                contentLength = 1234L,
                serverName = "TestServer/1.0",
                requestDuration = 500L,
                etag = "W/\"abc-123\"",
                requestId = "req-456",
                timestamp = 1234567890L,
            )

        assertThat(result.url).isEqualTo("https://api.example.com/test")
        assertThat(result.protocol).isEqualTo("h2")
        assertThat(result.statusCode).isEqualTo(200)
        assertThat(result.message).isEqualTo("OK")
        assertThat(result.contentType).isEqualTo("application/json")
        assertThat(result.contentLength).isEqualTo(1234L)
        assertThat(result.serverName).isEqualTo("TestServer/1.0")
        assertThat(result.requestDuration).isEqualTo(500L)
        assertThat(result.etag).isEqualTo("W/\"abc-123\"")
        assertThat(result.requestId).isEqualTo("req-456")
        assertThat(result.timestamp).isEqualTo(1234567890L)
    }

    @Test
    fun `constructor uses default timestamp when not provided`() {
        val beforeTimestamp = System.currentTimeMillis()

        val result =
            HttpResponseMetadata(
                url = "https://api.example.com/test",
                protocol = "h2",
                statusCode = 200,
                message = "OK",
                contentType = "application/json",
                contentLength = 1234L,
                serverName = "TestServer",
                requestDuration = 100L,
                etag = null,
                requestId = null,
            )

        val afterTimestamp = System.currentTimeMillis()

        assertThat(result.timestamp).isAtLeast(beforeTimestamp)
        assertThat(result.timestamp).isAtMost(afterTimestamp)
    }

    @Test
    fun `data class equality works correctly`() {
        val metadata1 =
            HttpResponseMetadata(
                url = "https://api.example.com",
                protocol = "h2",
                statusCode = 200,
                message = "OK",
                contentType = "application/json",
                contentLength = 100L,
                serverName = "Server",
                requestDuration = 50L,
                etag = "abc",
                requestId = "123",
                timestamp = 1000L,
            )

        val metadata2 =
            HttpResponseMetadata(
                url = "https://api.example.com",
                protocol = "h2",
                statusCode = 200,
                message = "OK",
                contentType = "application/json",
                contentLength = 100L,
                serverName = "Server",
                requestDuration = 50L,
                etag = "abc",
                requestId = "123",
                timestamp = 1000L,
            )

        assertThat(metadata1).isEqualTo(metadata2)
    }

    @Test
    fun `data class copy works correctly`() {
        val original =
            HttpResponseMetadata(
                url = "https://api.example.com",
                protocol = "h2",
                statusCode = 200,
                message = "OK",
                contentType = "application/json",
                contentLength = 100L,
                serverName = "Server",
                requestDuration = 50L,
                etag = "abc",
                requestId = "123",
                timestamp = 1000L,
            )

        val copied = original.copy(statusCode = 404, message = "Not Found")

        assertThat(copied.statusCode).isEqualTo(404)
        assertThat(copied.message).isEqualTo("Not Found")
        assertThat(copied.url).isEqualTo(original.url)
        assertThat(copied.protocol).isEqualTo(original.protocol)
    }
}
