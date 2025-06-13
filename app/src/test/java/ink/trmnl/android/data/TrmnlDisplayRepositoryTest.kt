package ink.trmnl.android.data

import com.google.common.truth.Truth.assertThat
import com.slack.eithernet.ApiResult
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.network.TrmnlApiService
import ink.trmnl.android.network.model.TrmnlCurrentImageResponse
import ink.trmnl.android.network.model.TrmnlDisplayResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TrmnlDisplayRepository].
 */
class TrmnlDisplayRepositoryTest {
    private lateinit var repository: TrmnlDisplayRepository
    private lateinit var apiService: TrmnlApiService
    private lateinit var imageMetadataStore: ImageMetadataStore
    private lateinit var repositoryConfigProvider: RepositoryConfigProvider
    private lateinit var deviceConfigDataStore: TrmnlDeviceConfigDataStore

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
        repositoryConfigProvider = mockk()
        deviceConfigDataStore = mockk()
        imageMetadataStore = mockk(relaxed = true)

        every { repositoryConfigProvider.shouldUseFakeData } returns false

        repository =
            TrmnlDisplayRepository(
                apiService = apiService,
                imageMetadataStore = imageMetadataStore,
                repositoryConfigProvider = repositoryConfigProvider,
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
                    imageName = "test-image.png",
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
                )
            } returns ApiResult.success(successResponse)

            // Act
            val result = repository.getNextDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).isEqualTo("https://test.com/image.png")
            assertThat(result.imageName).isEqualTo("test-image.png")
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
                    imageName = null,
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
                )
            } returns ApiResult.success(errorResponse)

            // Act
            val result = repository.getNextDisplayData(testDeviceConfig)

            // Assert
            assertThat(result.status).isEqualTo(500)
            assertThat(result.imageUrl).isEmpty()
            assertThat(result.imageName).isEmpty()
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
            assertThat(result.imageName).isEqualTo("current-image.png")
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
            assertThat(result.imageName).isEmpty()
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
            assertThat(result.imageName).contains("mocked-image-grayscale&time")
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
            assertThat(result.imageName).contains("mocked-image-grayscale&time")
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

            // Use reflection to access private method
            val method =
                TrmnlDisplayRepository::class.java.getDeclaredMethod(
                    "constructApiUrl",
                    String::class.java,
                    String::class.java,
                )
            method.isAccessible = true

            // Act & Assert
            assertThat(method.invoke(repository, urlWithSlash, "api/endpoint"))
                .isEqualTo(expectedWithSlash)

            assertThat(method.invoke(repository, urlWithoutSlash, "api/endpoint"))
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
                    imageName = "test-image.png",
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
                    imageName = "test-image.png",
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
}
