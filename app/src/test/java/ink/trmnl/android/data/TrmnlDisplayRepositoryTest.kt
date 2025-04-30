package ink.trmnl.android.data

import com.google.common.truth.Truth.assertThat
import com.slack.eithernet.ApiResult
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

    private val testAccessToken = "test-access-token"

    @Before
    fun setup() {
        apiService = mockk()
        repositoryConfigProvider = mockk()
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
            coEvery { apiService.getNextDisplayData(testAccessToken) } returns ApiResult.success(successResponse)

            // Act
            val result = repository.getNextDisplayData(testAccessToken)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).isEqualTo("https://test.com/image.png")
            assertThat(result.imageName).isEqualTo("test-image.png")
            assertThat(result.refreshIntervalSeconds).isEqualTo(300L)
            assertThat(result.error).isNull()

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
            coEvery { apiService.getNextDisplayData(testAccessToken) } returns ApiResult.success(errorResponse)

            // Act
            val result = repository.getNextDisplayData(testAccessToken)

            // Assert
            assertThat(result.status).isEqualTo(500)
            assertThat(result.imageUrl).isEmpty()
            assertThat(result.imageName).isEmpty()
            assertThat(result.refreshIntervalSeconds).isNull()
            assertThat(result.error).isEqualTo("Error fetching display")

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
            coEvery { apiService.getCurrentDisplayData(testAccessToken) } returns ApiResult.success(successResponse)

            // Act
            val result = repository.getCurrentDisplayData(testAccessToken)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).isEqualTo("https://test.com/current.png")
            assertThat(result.imageName).isEqualTo("current-image.png")
            assertThat(result.refreshIntervalSeconds).isEqualTo(600L)
            assertThat(result.error).isNull()

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
            coEvery { apiService.getCurrentDisplayData(testAccessToken) } returns ApiResult.success(errorResponse)

            // Act
            val result = repository.getCurrentDisplayData(testAccessToken)

            // Assert
            assertThat(result.status).isEqualTo(500)
            assertThat(result.imageUrl).isEmpty()
            assertThat(result.imageName).isEmpty()
            assertThat(result.refreshIntervalSeconds).isNull()
            assertThat(result.error).isEqualTo("Device not found")

            // Verify metadata was NOT saved
            coVerify(exactly = 0) { imageMetadataStore.saveImageMetadata(any(), any()) }
        }

    @Test
    fun `getNextDisplayData should return fake data when shouldUseFakeData is true`() =
        runTest {
            // Arrange
            every { repositoryConfigProvider.shouldUseFakeData } returns true

            // Act
            val result = repository.getNextDisplayData(testAccessToken)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).contains("picsum.photos")
            assertThat(result.imageName).contains("mocked-image-grayscale&time")
            assertThat(result.refreshIntervalSeconds).isEqualTo(600L)
            assertThat(result.error).isNull()

            // Verify API was NOT called
            coVerify(exactly = 0) { apiService.getNextDisplayData(any()) }
        }

    @Test
    fun `getCurrentDisplayData should return fake data when shouldUseFakeData is true`() =
        runTest {
            // Arrange
            every { repositoryConfigProvider.shouldUseFakeData } returns true

            // Act
            val result = repository.getCurrentDisplayData(testAccessToken)

            // Assert
            assertThat(result.status).isEqualTo(200)
            assertThat(result.imageUrl).contains("picsum.photos")
            assertThat(result.imageName).contains("mocked-image-grayscale&time")
            assertThat(result.refreshIntervalSeconds).isEqualTo(600L)
            assertThat(result.error).isNull()

            // Verify API was NOT called
            coVerify(exactly = 0) { apiService.getCurrentDisplayData(any()) }
        }
}
