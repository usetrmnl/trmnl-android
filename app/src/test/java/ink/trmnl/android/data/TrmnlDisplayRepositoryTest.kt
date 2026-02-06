package ink.trmnl.android.data

import com.google.common.truth.Truth.assertThat
import com.slack.eithernet.ApiResult
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.network.TrmnlApiService
import ink.trmnl.android.network.TrmnlUserApiService
import ink.trmnl.android.network.model.TrmnlCurrentImageResponse
import ink.trmnl.android.network.model.TrmnlDisplayResponse
import ink.trmnl.android.network.util.constructApiUrl
import ink.trmnl.android.util.AndroidDeviceInfoProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Unit tests for [TrmnlDisplayRepository].
 */
@OptIn(com.slack.eithernet.InternalEitherNetApi::class)
class TrmnlDisplayRepositoryTest {
    private lateinit var repository: TrmnlDisplayRepository
    private lateinit var apiService: TrmnlApiService
    private lateinit var userApiService: TrmnlUserApiService
    private lateinit var imageMetadataStore: ImageMetadataStore
    private lateinit var repositoryConfigProvider: RepositoryConfigProvider
    private lateinit var deviceConfigDataStore: TrmnlDeviceConfigDataStore
    private lateinit var androidDeviceInfoProvider: AndroidDeviceInfoProvider

    private val testDeviceConfig =
        TrmnlDeviceConfig(
            type = TrmnlDeviceType.TRMNL,
            apiAccessToken = "test-access-token",
            apiBaseUrl = "https://server.example.com",
            refreshRateSecs = 600L,
        )

    private val byosDeviceConfig =
        TrmnlDeviceConfig(
            type = TrmnlDeviceType.BYOS,
            apiAccessToken = "test-access-token",
            apiBaseUrl = "https://custom-server.example.com",
            refreshRateSecs = 600L,
        )

    private val byodDeviceConfig =
        TrmnlDeviceConfig(
            type = TrmnlDeviceType.BYOD,
            apiAccessToken = "test-access-token",
            apiBaseUrl = "https://server.example.com",
            refreshRateSecs = 600L,
        )

    @Before
    fun setup() {
        apiService = mockk()
        userApiService = mockk()
        repositoryConfigProvider = mockk()
        deviceConfigDataStore = mockk()
        imageMetadataStore = mockk(relaxed = true)
        androidDeviceInfoProvider = mockk(relaxed = true)

        every { repositoryConfigProvider.shouldUseFakeData } returns false

        repository =
            TrmnlDisplayRepository(
                apiService = apiService,
                userApiService = userApiService,
                imageMetadataStore = imageMetadataStore,
                repositoryConfigProvider = repositoryConfigProvider,
                androidDeviceInfoProvider = androidDeviceInfoProvider,
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getNextDisplayData should return mapped display info when API call succeeds`() =
        runTest {
            // Arrange
            val successResponse =
                TrmnlDisplayResponse(
                    status = 200,
                    imageUrl = "https://test.com/image.png",
                    imageFileName = "test-image.png",
                    refreshRate = 300L,
                    error = null,
                    updateFirmware = null,
                    firmwareUrl = null,
                    resetFirmware = null,
                )

            val expectedNextApiUrl = "https://server.example.com/api/display"

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = expectedNextApiUrl,
                    accessToken = testDeviceConfig.apiAccessToken,
                    useBase64 = any(),
                    rssi = any(),
                )
            } returns ApiResult.success(successResponse)

            // Act
            val result = repository.getNextDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).isEqualTo("https://test.com/image.png")
            assertThat(result.imageFileName).isEqualTo("test-image.png")
            assertThat(result.refreshIntervalSeconds).isEqualTo(300L)
            assertThat(result.error).isNull()
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)

            // Verify metadata was saved
            coVerify { imageMetadataStore.saveImageMetadata("https://test.com/image.png", 300L) }
        }

    @Test
    fun `getNextDisplayData should handle error response`() =
        runTest {
            // Arrange
            val errorResponse =
                TrmnlDisplayResponse(
                    status = 500,
                    imageUrl = null,
                    imageFileName = null,
                    refreshRate = null,
                    error = "Error fetching display",
                    updateFirmware = null,
                    firmwareUrl = null,
                    resetFirmware = null,
                )

            val expectedNextApiUrl = "https://server.example.com/api/display"

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = expectedNextApiUrl,
                    accessToken = testDeviceConfig.apiAccessToken,
                    useBase64 = any(),
                    rssi = any(),
                )
            } returns ApiResult.success(errorResponse)

            // Act
            val result = repository.getNextDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(500)
            assertThat(result.imageUrl).isEmpty()
            assertThat(result.imageFileName).isEmpty()
            assertThat(result.refreshIntervalSeconds).isNull()
            assertThat(result.error).isEqualTo("Error fetching display")
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)

            // Verify metadata was NOT saved
            coVerify(exactly = 0) { imageMetadataStore.saveImageMetadata(any(), any()) }
        }

    @Test
    fun `getCurrentDisplayData should return mapped display info when API call succeeds`() =
        runTest {
            // Arrange
            val successResponse =
                TrmnlCurrentImageResponse(
                    status = 200,
                    imageUrl = "https://test.com/current.png",
                    filename = "current-image.png",
                    refreshRateSec = 600L,
                    renderedAt = 1234567890L,
                    error = null,
                )

            val expectedCurrentApiUrl = "https://server.example.com/api/current_screen"

            coEvery {
                apiService.getCurrentDisplayData(
                    fullApiUrl = expectedCurrentApiUrl,
                    accessToken = testDeviceConfig.apiAccessToken,
                )
            } returns ApiResult.success(successResponse)

            // Act
            val result = repository.getCurrentDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).isEqualTo("https://test.com/current.png")
            assertThat(result.imageFileName).isEqualTo("current-image.png")
            assertThat(result.refreshIntervalSeconds).isEqualTo(600L)
            assertThat(result.error).isNull()
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)

            // Verify metadata was saved
            coVerify { imageMetadataStore.saveImageMetadata("https://test.com/current.png", 600L) }
        }

    @Test
    fun `getCurrentDisplayData should handle error response`() =
        runTest {
            // Arrange
            val errorResponse =
                TrmnlCurrentImageResponse(
                    status = 500,
                    imageUrl = null,
                    filename = null,
                    refreshRateSec = null,
                    renderedAt = null,
                    error = "Device not found",
                )

            val expectedCurrentApiUrl = "https://server.example.com/api/current_screen"

            coEvery {
                apiService.getCurrentDisplayData(
                    fullApiUrl = expectedCurrentApiUrl,
                    accessToken = testDeviceConfig.apiAccessToken,
                )
            } returns ApiResult.success(errorResponse)

            // Act
            val result = repository.getCurrentDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(500)
            assertThat(result.imageUrl).isEmpty()
            assertThat(result.imageFileName).isEmpty()
            assertThat(result.refreshIntervalSeconds).isNull()
            assertThat(result.error).isEqualTo("Device not found")
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)

            // Verify metadata was NOT saved
            coVerify(exactly = 0) { imageMetadataStore.saveImageMetadata(any(), any()) }
        }

    @Test
    fun `getNextDisplayData should return fake data when shouldUseFakeData is true`() =
        runTest {
            // Arrange
            every { repositoryConfigProvider.shouldUseFakeData } returns true

            // Act
            val result = repository.getNextDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).contains("picsum.photos")
            assertThat(result.imageFileName).contains("mocked-image-grayscale&time")
            assertThat(result.refreshIntervalSeconds).isEqualTo(600L)
            assertThat(result.error).isNull()

            // Verify API was NOT called
            coVerify(exactly = 0) { apiService.getNextDisplayData(any(), any()) }
        }

    @Test
    fun `getCurrentDisplayData should return fake data when shouldUseFakeData is true`() =
        runTest {
            // Arrange
            every { repositoryConfigProvider.shouldUseFakeData } returns true

            // Act
            val result = repository.getCurrentDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).contains("picsum.photos")
            assertThat(result.imageFileName).contains("mocked-image-grayscale&time")
            assertThat(result.refreshIntervalSeconds).isEqualTo(600L)
            assertThat(result.error).isNull()

            // Verify API was NOT called
            coVerify(exactly = 0) { apiService.getCurrentDisplayData(any(), any()) }
        }

    @Test
    fun `constructApiUrl should handle URLs with and without trailing slashes`() =
        runTest {
            // Test with trailing slash
            val urlWithSlash = "https://example.com/"
            val expectedWithSlash = "https://example.com/api/endpoint"

            // Test without trailing slash
            val urlWithoutSlash = "https://example.com"
            val expectedWithoutSlash = "https://example.com/api/endpoint"

            // Act & Assert
            assertThat(constructApiUrl(urlWithSlash, "api/endpoint"))
                .isEqualTo(expectedWithSlash)

            assertThat(constructApiUrl(urlWithoutSlash, "api/endpoint"))
                .isEqualTo(expectedWithoutSlash)
        }

    @Test
    fun `getCurrentDisplayData should warn when BYOS device type is used`() =
        runTest {
            // Arrange
            val successResponse =
                TrmnlCurrentImageResponse(
                    status = 200,
                    imageUrl = "https://test.com/current.png",
                    filename = "current-image.png",
                    refreshRateSec = 600L,
                    renderedAt = 1234567890L,
                    error = null,
                )

            val expectedCurrentApiUrl = "https://custom-server.example.com/api/current_screen"

            coEvery {
                apiService.getCurrentDisplayData(
                    fullApiUrl = expectedCurrentApiUrl,
                    accessToken = byosDeviceConfig.apiAccessToken,
                )
            } returns ApiResult.success(successResponse)

            // Act
            val result = repository.getCurrentDisplayData(byosDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).isEqualTo("https://test.com/current.png")
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.BYOS)

            // There should be a warning logged, but we can't easily test timber logs
        }

    @Test
    fun `getNextDisplayData with BYOS should use custom server URL`() =
        runTest {
            // Arrange
            val successResponse =
                TrmnlDisplayResponse(
                    status = 200,
                    imageUrl = "https://test.com/image.png",
                    imageFileName = "test-image.png",
                    refreshRate = 300L,
                    error = null,
                    updateFirmware = null,
                    firmwareUrl = null,
                    resetFirmware = null,
                )

            val expectedNextApiUrl = "https://custom-server.example.com/api/display"

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = expectedNextApiUrl,
                    accessToken = byosDeviceConfig.apiAccessToken,
                    useBase64 = any(),
                    rssi = any(),
                )
            } returns ApiResult.success(successResponse)

            // Act
            val result = repository.getNextDisplayData(byosDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).isEqualTo("https://test.com/image.png")
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.BYOS)

            // Verify the correct URL was constructed with the custom server
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = expectedNextApiUrl,
                    accessToken = byosDeviceConfig.apiAccessToken,
                    useBase64 = any(),
                    rssi = any(),
                )
            }
        }

    @Test
    fun `getNextDisplayData with BYOD should use default TRMNL API server URL`() =
        runTest {
            // Arrange
            val successResponse =
                TrmnlDisplayResponse(
                    status = 200,
                    imageUrl = "https://test.com/image.png",
                    imageFileName = "test-image.png",
                    refreshRate = 300L,
                    error = null,
                    updateFirmware = null,
                    firmwareUrl = null,
                    resetFirmware = null,
                )

            // Despite byodDeviceConfig having a server URL, the standard TRMNL API endpoint should be used
            val expectedNextApiUrl = "https://server.example.com/api/display"

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = expectedNextApiUrl,
                    accessToken = byodDeviceConfig.apiAccessToken,
                    useBase64 = any(),
                    rssi = any(),
                    percentCharged = any(),
                    deviceMacId = any(),
                )
            } returns ApiResult.success(successResponse)

            // Act
            val result = repository.getNextDisplayData(byodDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).isEqualTo("https://test.com/image.png")
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.BYOD)

            // Verify the correct URL was used
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = expectedNextApiUrl,
                    accessToken = byodDeviceConfig.apiAccessToken,
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = any(),
                    percentCharged = any(),
                )
            }
        }

    @Test
    fun `getCurrentDisplayData should use custom server URL for BYOS`() =
        runTest {
            // Arrange
            val successResponse =
                TrmnlCurrentImageResponse(
                    status = 200,
                    imageUrl = "https://test.com/current.png",
                    filename = "current-image.png",
                    refreshRateSec = 600L,
                    renderedAt = 1234567890L,
                    error = null,
                )

            val expectedCurrentApiUrl = "https://custom-server.example.com/api/current_screen"

            coEvery {
                apiService.getCurrentDisplayData(
                    fullApiUrl = expectedCurrentApiUrl,
                    accessToken = byosDeviceConfig.apiAccessToken,
                )
            } returns ApiResult.success(successResponse)

            // Act
            val result = repository.getCurrentDisplayData(byosDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).isEqualTo("https://test.com/current.png")
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.BYOS)

            // Verify the custom server URL was used
            coVerify {
                apiService.getCurrentDisplayData(
                    fullApiUrl = expectedCurrentApiUrl,
                    accessToken = byosDeviceConfig.apiAccessToken,
                )
            }
        }

    @Test
    fun `getNextDisplayData should extract HTTP metadata from failure response`() =
        runTest {
            // Arrange
            val expectedNextApiUrl = "https://server.example.com/api/display"

            // Create mock Response with proper data
            val mockRequest = Request.Builder().url(expectedNextApiUrl).build()
            val mockResponse =
                mockk<Response> {
                    every { request } returns mockRequest
                    every { protocol } returns Protocol.HTTP_2
                    every { code } returns 429
                    every { message } returns "Too Many Requests"
                    every { header("Content-Type") } returns "text/html"
                    every { header("Content-Length") } returns null
                    every { header("Server") } returns "cloudflare"
                    every { header("etag") } returns null
                    every { header("x-request-id") } returns "e9f73be5-bf47-4c30-9c87-60990c86447a"
                    every { sentRequestAtMillis } returns 1000L
                    every { receivedResponseAtMillis } returns 1050L
                }

            // Create HttpFailure with tags containing the Response
            val httpFailure: ApiResult<TrmnlDisplayResponse, Unit> =
                ApiResult.Failure.HttpFailure(
                    code = 429,
                    error = Unit,
                    tags = mapOf(Response::class to mockResponse),
                )

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = expectedNextApiUrl,
                    accessToken = testDeviceConfig.apiAccessToken,
                    useBase64 = any(),
                    rssi = any(),
                )
            } returns httpFailure

            // Act
            val result = repository.getNextDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(429)
            assertThat(result.error).contains("HTTP failure: 429")
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)
            // Verify HTTP metadata is extracted
            assertThat(result.httpResponseMetadata).isNotNull()
            assertThat(result.httpResponseMetadata?.url).isEqualTo(expectedNextApiUrl)
            assertThat(result.httpResponseMetadata?.statusCode).isEqualTo(429)
            assertThat(result.httpResponseMetadata?.protocol).isEqualTo("h2")
            assertThat(result.httpResponseMetadata?.requestDuration).isEqualTo(50L)
        }

    @Test
    fun `getCurrentDisplayData should extract HTTP metadata from failure response`() =
        runTest {
            // Arrange
            val expectedCurrentApiUrl = "https://server.example.com/api/current_screen"

            // Create mock Response with proper data
            val mockRequest = Request.Builder().url(expectedCurrentApiUrl).build()
            val mockResponse =
                mockk<Response> {
                    every { request } returns mockRequest
                    every { protocol } returns Protocol.HTTP_2
                    every { code } returns 500
                    every { message } returns "Internal Server Error"
                    every { header("Content-Type") } returns "application/json"
                    every { header("Content-Length") } returns "123"
                    every { header("Server") } returns null
                    every { header("etag") } returns null
                    every { header("x-request-id") } returns "abc-123"
                    every { sentRequestAtMillis } returns 2000L
                    every { receivedResponseAtMillis } returns 2100L
                }

            // Create HttpFailure with tags containing the Response
            val httpFailure: ApiResult<TrmnlCurrentImageResponse, Unit> =
                ApiResult.Failure.HttpFailure(
                    code = 500,
                    error = Unit,
                    tags = mapOf(Response::class to mockResponse),
                )

            coEvery {
                apiService.getCurrentDisplayData(
                    fullApiUrl = expectedCurrentApiUrl,
                    accessToken = testDeviceConfig.apiAccessToken,
                )
            } returns httpFailure

            // Act
            val result = repository.getCurrentDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(500)
            assertThat(result.error).contains("HTTP failure: 500")
            assertThat(result.trmnlDeviceType).isEqualTo(TrmnlDeviceType.TRMNL)
            // Verify HTTP metadata is extracted
            assertThat(result.httpResponseMetadata).isNotNull()
            assertThat(result.httpResponseMetadata?.url).isEqualTo(expectedCurrentApiUrl)
            assertThat(result.httpResponseMetadata?.statusCode).isEqualTo(500)
            assertThat(result.httpResponseMetadata?.contentLength).isEqualTo(123L)
        }

    // DEPRECATED: Device ID fetching is no longer needed for battery reporting
    // Battery is now sent via Percent-Charged header
    @Ignore("Device ID fetching deprecated - battery now uses Percent-Charged header")
    @Test
    fun `getDeviceIdFromApi should return mocked device ID`() =
        runTest {
            // Act
            val result = repository.getDeviceIdFromApi(byodDeviceConfig)

            // Assert
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(41448)

            // Verify API was NOT called since we're using mocked response
            coVerify(exactly = 0) { apiService.getDeviceMe(any(), any()) }
        }

    // DEPRECATED: Battery reporting via separate API call is no longer used
    // Battery is now sent via Percent-Charged header in /api/display call
    @Ignore("Battery reporting via PATCH /api/devices/{id} deprecated - now uses Percent-Charged header")
    @Test
    fun `reportDeviceBatteryStatus should report battery for valid BYOD config`() =
        runTest {
            // Arrange
            val byodConfigWithDeviceId =
                byodDeviceConfig.copy(
                    deviceId = 123,
                    userApiToken = "user_test_token",
                )

            every { androidDeviceInfoProvider.getBatteryLevel() } returns 75

            val expectedApiUrl = "https://server.example.com/api/devices/123"

            coEvery {
                userApiService.updateDevice(
                    fullApiUrl = expectedApiUrl,
                    accessToken = "Bearer user_test_token",
                    updateRequest = any(),
                )
            } returns ApiResult.success(mockk(relaxed = true))

            // Act
            repository.reportDeviceBatteryStatus(byodConfigWithDeviceId)

            // Assert - Verify battery status was reported
            coVerify {
                userApiService.updateDevice(
                    fullApiUrl = expectedApiUrl,
                    accessToken = "Bearer user_test_token",
                    updateRequest = match { it.percentCharged == 75.0 },
                )
            }
        }

    // DEPRECATED: Battery reporting via separate API call is no longer used
    @Ignore("Battery reporting via PATCH /api/devices/{id} deprecated - now uses Percent-Charged header")
    @Test
    fun `reportDeviceBatteryStatus should skip for non-BYOD device`() =
        runTest {
            // Arrange - TRMNL device (not BYOD)
            val trmnlConfig =
                testDeviceConfig.copy(
                    deviceId = 123,
                    userApiToken = "user_test_token",
                )

            // Act
            repository.reportDeviceBatteryStatus(trmnlConfig)

            // Assert - Verify API was NOT called
            coVerify(exactly = 0) { userApiService.updateDevice(any(), any(), any()) }
            coVerify(exactly = 0) { androidDeviceInfoProvider.getBatteryLevel() }
        }

    // DEPRECATED: Battery reporting via separate API call is no longer used
    @Ignore("Battery reporting via PATCH /api/devices/{id} deprecated - now uses Percent-Charged header")
    @Test
    fun `reportDeviceBatteryStatus should skip when deviceId is null`() =
        runTest {
            // Arrange
            val configWithoutDeviceId =
                byodDeviceConfig.copy(
                    deviceId = null,
                    userApiToken = "user_test_token",
                )

            // Act
            repository.reportDeviceBatteryStatus(configWithoutDeviceId)

            // Assert - Verify API was NOT called
            coVerify(exactly = 0) { userApiService.updateDevice(any(), any(), any()) }
            coVerify(exactly = 0) { androidDeviceInfoProvider.getBatteryLevel() }
        }

    // DEPRECATED: Battery reporting via separate API call is no longer used
    @Ignore("Battery reporting via PATCH /api/devices/{id} deprecated - now uses Percent-Charged header")
    @Test
    fun `reportDeviceBatteryStatus should skip when userApiToken is null`() =
        runTest {
            // Arrange
            val configWithoutUserToken =
                byodDeviceConfig.copy(
                    deviceId = 123,
                    userApiToken = null,
                )

            // Act
            repository.reportDeviceBatteryStatus(configWithoutUserToken)

            // Assert - Verify API was NOT called
            coVerify(exactly = 0) { userApiService.updateDevice(any(), any(), any()) }
            coVerify(exactly = 0) { androidDeviceInfoProvider.getBatteryLevel() }
        }

    // DEPRECATED: Battery reporting via separate API call is no longer used
    @Ignore("Battery reporting via PATCH /api/devices/{id} deprecated - now uses Percent-Charged header")
    @Test
    fun `reportDeviceBatteryStatus should skip when battery level unavailable`() =
        runTest {
            // Arrange
            val byodConfigWithDeviceId =
                byodDeviceConfig.copy(
                    deviceId = 123,
                    userApiToken = "user_test_token",
                )

            every { androidDeviceInfoProvider.getBatteryLevel() } returns null

            // Act
            repository.reportDeviceBatteryStatus(byodConfigWithDeviceId)

            // Assert - Verify battery level was requested but API was NOT called
            coVerify(exactly = 1) { androidDeviceInfoProvider.getBatteryLevel() }
            coVerify(exactly = 0) { userApiService.updateDevice(any(), any(), any()) }
        }

    // WiFi Signal Strength (RSSI) Tests

    @Test
    fun `getNextDisplayData should send RSSI for BYOD device when WiFi available`() =
        runTest {
            // Arrange
            val byodConfig =
                byodDeviceConfig.copy(
                    deviceId = null,
                    userApiToken = "test_token",
                    apiAccessToken = "test_api_key",
                )
            val expectedRssi = -65
            val expectedBattery = 80

            every { androidDeviceInfoProvider.getWifiSignalStrength() } returns expectedRssi
            every { androidDeviceInfoProvider.getBatteryLevel() } returns expectedBattery

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = expectedRssi,
                    percentCharged = expectedBattery.toDouble(),
                )
            } returns ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(byodConfig)

            // Assert - Verify RSSI was fetched and sent
            coVerify(exactly = 1) { androidDeviceInfoProvider.getWifiSignalStrength() }
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = expectedRssi,
                    percentCharged = expectedBattery.toDouble(),
                )
            }
        }

    @Test
    fun `getNextDisplayData should send null RSSI for BYOD when WiFi unavailable`() =
        runTest {
            // Arrange
            val byodConfig =
                byodDeviceConfig.copy(
                    deviceId = null,
                    userApiToken = "test_token",
                    apiAccessToken = "test_api_key",
                )
            val expectedBattery = 80

            every { androidDeviceInfoProvider.getWifiSignalStrength() } returns null
            every { androidDeviceInfoProvider.getBatteryLevel() } returns expectedBattery

            coEvery { apiService.getNextDisplayData(any(), any(), any(), any(), any(), any()) } returns
                ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(byodConfig)

            // Assert - Verify RSSI was fetched but null was sent
            coVerify(exactly = 1) { androidDeviceInfoProvider.getWifiSignalStrength() }
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = null,
                    percentCharged = expectedBattery.toDouble(),
                )
            }
        }

    @Test
    fun `getNextDisplayData should NOT send RSSI for TRMNL device`() =
        runTest {
            // Arrange - TRMNL device (not BYOD)
            val trmnlConfig =
                testDeviceConfig.copy(
                    apiAccessToken = "trmnl_api_key",
                )

            coEvery { apiService.getNextDisplayData(any(), any(), any(), any(), any(), any()) } returns
                ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(trmnlConfig)

            // Assert - Verify WiFi signal was NOT fetched and null RSSI was sent
            coVerify(exactly = 0) { androidDeviceInfoProvider.getWifiSignalStrength() }
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = null,
                    percentCharged = null,
                )
            }
        }

    @Test
    fun `getNextDisplayData should NOT send RSSI for BYOS device`() =
        runTest {
            // Arrange - BYOS device uses next display data endpoint (not current_screen)
            val byosConfig =
                byosDeviceConfig.copy(
                    apiAccessToken = "byos_api_key",
                )

            coEvery { apiService.getNextDisplayData(any(), any(), any(), any(), any()) } returns
                ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(byosConfig)

            // Assert - Verify WiFi signal was NOT fetched for BYOS device
            coVerify(exactly = 0) { androidDeviceInfoProvider.getWifiSignalStrength() }
            // Verify null RSSI was sent
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = null,
                )
            }
        }

    // Battery Percentage (Percent-Charged Header) Tests

    @Test
    fun `getNextDisplayData should send battery percentage for BYOD device when available`() =
        runTest {
            // Arrange
            val byodConfig =
                byodDeviceConfig.copy(
                    apiAccessToken = "test_api_key",
                )
            val expectedBatteryLevel = 75

            every { androidDeviceInfoProvider.getBatteryLevel() } returns expectedBatteryLevel
            every { androidDeviceInfoProvider.getWifiSignalStrength() } returns -65

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = any(),
                    percentCharged = 75.0,
                )
            } returns ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(byodConfig)

            // Assert - Verify battery level was fetched and sent as header
            coVerify(exactly = 1) { androidDeviceInfoProvider.getBatteryLevel() }
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = any(),
                    percentCharged = 75.0,
                )
            }
        }

    @Test
    fun `getNextDisplayData should send null battery percentage for BYOD when unavailable`() =
        runTest {
            // Arrange
            val byodConfig =
                byodDeviceConfig.copy(
                    apiAccessToken = "test_api_key",
                )

            every { androidDeviceInfoProvider.getBatteryLevel() } returns null
            every { androidDeviceInfoProvider.getWifiSignalStrength() } returns -65

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = any(),
                    percentCharged = null,
                )
            } returns ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(byodConfig)

            // Assert - Verify battery level was fetched but null was sent
            coVerify(exactly = 1) { androidDeviceInfoProvider.getBatteryLevel() }
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = any(),
                    percentCharged = null,
                )
            }
        }

    @Test
    fun `getNextDisplayData should NOT send battery percentage for TRMNL device`() =
        runTest {
            // Arrange - TRMNL device (not BYOD)
            val trmnlConfig =
                testDeviceConfig.copy(
                    apiAccessToken = "trmnl_api_key",
                )

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = null,
                    percentCharged = null,
                )
            } returns ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(trmnlConfig)

            // Assert - Verify battery level was NOT fetched and null was sent
            coVerify(exactly = 0) { androidDeviceInfoProvider.getBatteryLevel() }
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = null,
                    percentCharged = null,
                )
            }
        }

    @Test
    fun `getNextDisplayData should NOT send battery percentage for BYOS device`() =
        runTest {
            // Arrange - BYOS device
            val byosConfig =
                byosDeviceConfig.copy(
                    apiAccessToken = "byos_api_key",
                )

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = null,
                    percentCharged = null,
                )
            } returns ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(byosConfig)

            // Assert - Verify battery level was NOT fetched for BYOS device
            coVerify(exactly = 0) { androidDeviceInfoProvider.getBatteryLevel() }
            // Verify null battery percentage was sent
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = null,
                    percentCharged = null,
                )
            }
        }

    @Test
    fun `getNextDisplayData should call getWifiSignalStrength only for BYOD devices`() =
        runTest {
            // Arrange - Multiple device types
            val byodConfig = byodDeviceConfig.copy(apiAccessToken = "byod_key")
            val trmnlConfig = testDeviceConfig.copy(apiAccessToken = "trmnl_key")
            val byosConfig = byosDeviceConfig.copy(apiAccessToken = "byos_key")

            every { androidDeviceInfoProvider.getWifiSignalStrength() } returns -70
            every { androidDeviceInfoProvider.getBatteryLevel() } returns 75

            coEvery { apiService.getNextDisplayData(any(), any(), any(), any(), any(), any()) } returns
                ApiResult.success(mockk(relaxed = true))

            // Act - Fetch for all device types
            repository.getNextDisplayData(byodConfig)
            repository.getNextDisplayData(trmnlConfig)
            repository.getNextDisplayData(byosConfig)

            // Assert - Verify WiFi signal was called only once (for BYOD)
            coVerify(exactly = 1) { androidDeviceInfoProvider.getWifiSignalStrength() }
        }

    @Test
    fun `getNextDisplayData should include RSSI in header for BYOD with strong signal`() =
        runTest {
            // Arrange
            val byodConfig = byodDeviceConfig.copy(apiAccessToken = "test_key")
            val strongSignal = -30 // Excellent signal
            val expectedBattery = 80

            every { androidDeviceInfoProvider.getWifiSignalStrength() } returns strongSignal
            every { androidDeviceInfoProvider.getBatteryLevel() } returns expectedBattery

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = strongSignal,
                    percentCharged = expectedBattery.toDouble(),
                )
            } returns ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(byodConfig)

            // Assert
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = strongSignal,
                    percentCharged = expectedBattery.toDouble(),
                )
            }
        }

    @Test
    fun `getNextDisplayData should include RSSI in header for BYOD with weak signal`() =
        runTest {
            // Arrange
            val byodConfig = byodDeviceConfig.copy(apiAccessToken = "test_key")
            val weakSignal = -90 // Very weak signal
            val expectedBattery = 80

            every { androidDeviceInfoProvider.getWifiSignalStrength() } returns weakSignal
            every { androidDeviceInfoProvider.getBatteryLevel() } returns expectedBattery

            coEvery {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = weakSignal,
                    percentCharged = expectedBattery.toDouble(),
                )
            } returns ApiResult.success(mockk(relaxed = true))

            // Act
            repository.getNextDisplayData(byodConfig)

            // Assert
            coVerify {
                apiService.getNextDisplayData(
                    fullApiUrl = any(),
                    accessToken = any(),
                    deviceMacId = any(),
                    useBase64 = any(),
                    rssi = weakSignal,
                    percentCharged = expectedBattery.toDouble(),
                )
            }
        }
}
