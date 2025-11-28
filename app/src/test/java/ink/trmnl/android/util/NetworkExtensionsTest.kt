package ink.trmnl.android.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for NetworkExtensions helper functions.
 */
class NetworkExtensionsTest {
    @Test
    fun `isHttpOk returns true for HTTP_OK status code`() {
        assertThat(HTTP_OK.isHttpOk()).isTrue()
    }

    @Test
    fun `isHttpOk returns true for HTTP_200 status code`() {
        assertThat(HTTP_200.isHttpOk()).isTrue()
    }

    @Test
    fun `isHttpOk returns true for HTTP_NONE status code`() {
        assertThat(HTTP_NONE.isHttpOk()).isTrue()
    }

    @Test
    fun `isHttpOk returns false for HTTP_500 status code`() {
        assertThat(HTTP_500.isHttpOk()).isFalse()
    }

    @Test
    fun `isHttpOk returns false for HTTP_429 status code`() {
        assertThat(HTTP_429.isHttpOk()).isFalse()
    }

    @Test
    fun `isHttpOk returns false for null status code`() {
        val nullCode: Int? = null
        assertThat(nullCode.isHttpOk()).isFalse()
    }

    @Test
    fun `isHttpError returns true for HTTP_500 status code`() {
        assertThat(HTTP_500.isHttpError()).isTrue()
    }

    @Test
    fun `isHttpError returns true for null status code`() {
        val nullCode: Int? = null
        assertThat(nullCode.isHttpError()).isTrue()
    }

    @Test
    fun `isHttpError returns false for HTTP_OK status code`() {
        assertThat(HTTP_OK.isHttpError()).isFalse()
    }

    @Test
    fun `isHttpError returns false for HTTP_200 status code`() {
        assertThat(HTTP_200.isHttpError()).isFalse()
    }

    @Test
    fun `isHttpError returns false for HTTP_429 status code`() {
        assertThat(HTTP_429.isHttpError()).isFalse()
    }

    @Test
    fun `isRateLimitError returns true for HTTP_429 status code`() {
        assertThat(HTTP_429.isRateLimitError()).isTrue()
    }

    @Test
    fun `isRateLimitError returns false for HTTP_500 status code`() {
        assertThat(HTTP_500.isRateLimitError()).isFalse()
    }

    @Test
    fun `isRateLimitError returns false for HTTP_200 status code`() {
        assertThat(HTTP_200.isRateLimitError()).isFalse()
    }

    @Test
    fun `isRateLimitError returns false for HTTP_OK status code`() {
        assertThat(HTTP_OK.isRateLimitError()).isFalse()
    }

    @Test
    fun `isRateLimitError returns false for null status code`() {
        val nullCode: Int? = null
        assertThat(nullCode.isRateLimitError()).isFalse()
    }

    @Test
    fun `HTTP constants have correct values`() {
        assertThat(HTTP_500).isEqualTo(500)
        assertThat(HTTP_200).isEqualTo(200)
        assertThat(HTTP_429).isEqualTo(429)
        assertThat(HTTP_OK).isEqualTo(0)
        assertThat(HTTP_NONE).isEqualTo(-1)
    }

    @Test
    fun `ERROR_TYPE_DEVICE_SETUP_REQUIRED has correct value`() {
        assertThat(ERROR_TYPE_DEVICE_SETUP_REQUIRED).isEqualTo("device_requires_setup")
    }
}
