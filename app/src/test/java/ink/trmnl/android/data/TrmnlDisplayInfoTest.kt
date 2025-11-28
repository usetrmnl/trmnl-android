package ink.trmnl.android.data

import com.google.common.truth.Truth.assertThat
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.util.ERROR_TYPE_DEVICE_SETUP_REQUIRED
import ink.trmnl.android.util.HTTP_200
import ink.trmnl.android.util.HTTP_500
import org.junit.Test

/**
 * Unit tests for TrmnlDisplayInfo data class and its companion object methods.
 */
class TrmnlDisplayInfoTest {
    @Test
    fun `setupRequired creates display info with correct values`() {
        val result = TrmnlDisplayInfo.setupRequired()

        assertThat(result.status).isEqualTo(HTTP_500)
        assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.BYOS)
        assertThat(result.imageUrl).isEmpty()
        assertThat(result.imageFileName).isEqualTo(ERROR_TYPE_DEVICE_SETUP_REQUIRED)
        assertThat(result.error).isEqualTo("Device setup required")
        assertThat(result.refreshIntervalSeconds).isEqualTo(0L)
    }

    @Test
    fun `setupRequired creates display info without http metadata`() {
        val result = TrmnlDisplayInfo.setupRequired()

        assertThat(result.httpResponseMetadata).isNull()
    }

    @Test
    fun `constructor creates display info with all provided values`() {
        val httpMetadata =
            HttpResponseMetadata(
                url = "https://api.example.com/display",
                protocol = "h2",
                statusCode = 200,
                message = "OK",
                contentType = "application/json",
                contentLength = 1234L,
                serverName = "TestServer",
                requestDuration = 500L,
                etag = "abc",
                requestId = "123",
            )

        val result =
            TrmnlDisplayInfo(
                status = HTTP_200,
                trmnlDeviceType = TrmnlDeviceType.TRMNL,
                imageUrl = "https://test.com/image.png",
                imageFileName = "test-image.png",
                error = null,
                refreshIntervalSeconds = 600L,
                httpResponseMetadata = httpMetadata,
            )

        assertThat(result.status).isEqualTo(HTTP_200)
        assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)
        assertThat(result.imageUrl).isEqualTo("https://test.com/image.png")
        assertThat(result.imageFileName).isEqualTo("test-image.png")
        assertThat(result.error).isNull()
        assertThat(result.refreshIntervalSeconds).isEqualTo(600L)
        assertThat(result.httpResponseMetadata).isEqualTo(httpMetadata)
    }

    @Test
    fun `constructor uses default refresh interval when not provided`() {
        val result =
            TrmnlDisplayInfo(
                status = HTTP_200,
                trmnlDeviceType = TrmnlDeviceType.TRMNL,
                imageUrl = "https://test.com/image.png",
                imageFileName = "test-image.png",
            )

        assertThat(result.refreshIntervalSeconds).isEqualTo(AppConfig.DEFAULT_REFRESH_INTERVAL_SEC)
    }

    @Test
    fun `constructor creates display info with error`() {
        val result =
            TrmnlDisplayInfo(
                status = HTTP_500,
                trmnlDeviceType = TrmnlDeviceType.BYOS,
                imageUrl = "",
                imageFileName = "",
                error = "Device not found",
                refreshIntervalSeconds = null,
            )

        assertThat(result.status).isEqualTo(HTTP_500)
        assertThat(result.error).isEqualTo("Device not found")
        assertThat(result.imageUrl).isEmpty()
        assertThat(result.imageFileName).isEmpty()
        assertThat(result.refreshIntervalSeconds).isNull()
    }

    @Test
    fun `data class equality works correctly`() {
        val info1 =
            TrmnlDisplayInfo(
                status = HTTP_200,
                trmnlDeviceType = TrmnlDeviceType.TRMNL,
                imageUrl = "https://test.com/image.png",
                imageFileName = "test.png",
                error = null,
                refreshIntervalSeconds = 600L,
            )

        val info2 =
            TrmnlDisplayInfo(
                status = HTTP_200,
                trmnlDeviceType = TrmnlDeviceType.TRMNL,
                imageUrl = "https://test.com/image.png",
                imageFileName = "test.png",
                error = null,
                refreshIntervalSeconds = 600L,
            )

        assertThat(info1).isEqualTo(info2)
    }

    @Test
    fun `data class copy works correctly`() {
        val original =
            TrmnlDisplayInfo(
                status = HTTP_200,
                trmnlDeviceType = TrmnlDeviceType.TRMNL,
                imageUrl = "https://test.com/image.png",
                imageFileName = "test.png",
            )

        val copied = original.copy(status = HTTP_500, error = "Error occurred")

        assertThat(copied.status).isEqualTo(HTTP_500)
        assertThat(copied.error).isEqualTo("Error occurred")
        assertThat(copied.imageUrl).isEqualTo(original.imageUrl)
        assertThat(copied.trmnlDeviceType).isEqualTo(original.trmnlDeviceType)
    }

    @Test
    fun `constructor creates display info for each device type`() {
        val trmnlInfo =
            TrmnlDisplayInfo(
                status = HTTP_200,
                trmnlDeviceType = TrmnlDeviceType.TRMNL,
                imageUrl = "https://test.com/trmnl.png",
                imageFileName = "trmnl.png",
            )

        val byosInfo =
            TrmnlDisplayInfo(
                status = HTTP_200,
                trmnlDeviceType = TrmnlDeviceType.BYOS,
                imageUrl = "https://test.com/byos.png",
                imageFileName = "byos.png",
            )

        val byodInfo =
            TrmnlDisplayInfo(
                status = HTTP_200,
                trmnlDeviceType = TrmnlDeviceType.BYOD,
                imageUrl = "https://test.com/byod.png",
                imageFileName = "byod.png",
            )

        assertThat(trmnlInfo.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)
        assertThat(byosInfo.trmnlDeviceType).isEqualTo(TrmnlDeviceType.BYOS)
        assertThat(byodInfo.trmnlDeviceType).isEqualTo(TrmnlDeviceType.BYOD)
    }
}
