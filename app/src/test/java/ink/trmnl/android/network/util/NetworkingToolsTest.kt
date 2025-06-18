package ink.trmnl.android.network.util

import com.google.common.truth.Truth.assertThat
import com.slack.eithernet.ApiResult
import com.slack.eithernet.InternalEitherNetApi
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Test

@OptIn(InternalEitherNetApi::class)
class NetworkingToolsTest {
    @Test
    fun `constructApiUrl should append endpoint correctly when baseUrl has trailing slash`() {
        // Arrange
        val baseUrl = "https://api.example.com/"
        val endpoint = "v1/data"

        // Act
        val result = constructApiUrl(baseUrl, endpoint)

        // Assert
        assertThat(result).isEqualTo("https://api.example.com/v1/data")
    }

    @Test
    fun `constructApiUrl should append endpoint correctly when baseUrl does not have trailing slash`() {
        // Arrange
        val baseUrl = "https://api.example.com"
        val endpoint = "v1/data"

        // Act
        val result = constructApiUrl(baseUrl, endpoint)

        // Assert
        assertThat(result).isEqualTo("https://api.example.com/v1/data")
    }

    @Test
    fun `constructApiUrl should handle empty endpoint`() {
        // Arrange
        val baseUrl = "https://api.example.com/"
        val endpoint = ""

        // Act
        val result = constructApiUrl(baseUrl, endpoint)

        // Assert
        assertThat(result).isEqualTo("https://api.example.com/")
    }

    @Test
    fun `extractHttpResponseMetadata should return correct metadata from ApiResult`() {
        // Arrange
        val requestX = Request.Builder().url("https://api.example.com/test").build()
        val mockResponse =
            mockk<Response> {
                every { request } returns requestX
                every { protocol } returns Protocol.HTTP_2
                every { code } returns 200
                every { message } returns "OK"
                every { header("Content-Type") } returns "application/json"
                every { header("Content-Length") } returns "1234"
                every { header("x-request-id") } returns "1"
                every { header("Server") } returns "TestServer/1.0"
                every { header("etag") } returns "W/\"abc-123\""
                every { sentRequestAtMillis } returns 1000L
                every { receivedResponseAtMillis } returns 1500L
            }

        val apiResult = ApiResult.Success("", mapOf(Response::class to mockResponse))

        // Act
        val metadata = extractHttpResponseMetadata(apiResult)

        // Assert
        assertThat(metadata).isNotNull()
        assertThat(metadata?.url).isEqualTo("https://api.example.com/test")
        assertThat(metadata?.protocol).isEqualTo("h2")
        assertThat(metadata?.statusCode).isEqualTo(200)
        assertThat(metadata?.message).isEqualTo("OK")
        assertThat(metadata?.contentType).isEqualTo("application/json")
        assertThat(metadata?.contentLength).isEqualTo(1234L)
        assertThat(metadata?.serverName).isEqualTo("TestServer/1.0")
        assertThat(metadata?.requestDuration).isEqualTo(500L)
        assertThat(metadata?.etag).isEqualTo("W/\"abc-123\"")
    }

    @Test
    fun `extractHttpResponseMetadata should return null when response is not in tags`() {
        // Arrange
        // val apiResult = ApiResult.success("SuccessData", emptyMap()) // No response in tags
        val apiResult = ApiResult.success(emptyMap<Class<*>, Any>())

        // Act
        val metadata = extractHttpResponseMetadata(apiResult)

        // Assert
        assertThat(metadata).isNull()
    }

    @Test
    fun `extractHttpResponseMetadata should handle missing headers gracefully`() {
        // Arrange
        val requestX = Request.Builder().url("https://api.example.com/test").build()
        val mockResponse =
            mockk<Response> {
                every { request } returns requestX
                every { protocol } returns Protocol.HTTP_1_1
                every { code } returns 404
                every { message } returns "Not Found"
                every { header("Content-Type") } returns null
                every { header("x-request-id") } returns null
                every { header("Content-Length") } returns null
                every { header("Server") } returns null
                every { header("etag") } returns null
                every { sentRequestAtMillis } returns 2000L
                every { receivedResponseAtMillis } returns 2100L
            }

        val apiResult = ApiResult.Success("", mapOf(Response::class to mockResponse))

        // Act
        val metadata = extractHttpResponseMetadata(apiResult)

        // Assert
        assertThat(metadata).isNotNull()
        assertThat(metadata?.contentType).isNull()
        assertThat(metadata?.contentLength).isEqualTo(-1L)
        assertThat(metadata?.serverName).isNull()
        assertThat(metadata?.etag).isNull()
        assertThat(metadata?.statusCode).isEqualTo(404)
        assertThat(metadata?.requestDuration).isEqualTo(100L)
    }
}
