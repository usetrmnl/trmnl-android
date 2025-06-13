package ink.trmnl.android.work

import com.google.common.truth.Truth.assertThat
import ink.trmnl.android.data.ImageMetadata
import ink.trmnl.android.data.ImageMetadataStore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class TrmnlImageUpdateManagerTest {
    // Mock dependencies
    private lateinit var imageMetadataStore: ImageMetadataStore

    // Class under test
    private lateinit var imageUpdateManager: TrmnlImageUpdateManager

    // Test data
    private val testImageUrl = "https://test.com/image.png"
    private val testRefreshIntervalSecs = 600L
    private val testErrorMessage = "Test error message"
    private val testImageMetadata =
        ImageMetadata(
            url = testImageUrl,
            refreshIntervalSecs = testRefreshIntervalSecs,
            errorMessage = null,
        )

    @Before
    fun setUp() {
        // Initialize mocks
        imageMetadataStore = mockk(relaxed = true)

        // Create instance of the class under test
        imageUpdateManager = TrmnlImageUpdateManager(imageMetadataStore)
    }

    @After
    fun tearDown() {
        // No explicit cleanup needed
    }

    @Test
    fun `updateImage should update the imageUpdateFlow with correct metadata`() =
        runTest {
            // Arrange - No arrangement needed, default setUp is sufficient

            // Act - Update the image
            imageUpdateManager.updateImage(
                imageUrl = testImageUrl,
                refreshIntervalSecs = testRefreshIntervalSecs,
            )

            // Assert - Check that the flow emits the correct value
            val result = imageUpdateManager.imageUpdateFlow.first()
            assertThat(result).isNotNull()
            assertThat(result?.url).isEqualTo(testImageUrl)
            assertThat(result?.refreshIntervalSecs).isEqualTo(testRefreshIntervalSecs)
            assertThat(result?.errorMessage).isNull()
        }

    @Test
    fun `updateImage should handle error message`() =
        runTest {
            // Act - Update the image with an error message
            imageUpdateManager.updateImage(
                imageUrl = testImageUrl,
                refreshIntervalSecs = null,
                errorMessage = testErrorMessage,
            )

            // Assert - Check that the flow emits the correct value with error message
            val result = imageUpdateManager.imageUpdateFlow.first()
            assertThat(result).isNotNull()
            assertThat(result?.url).isEqualTo(testImageUrl)
            assertThat(result?.refreshIntervalSecs).isNull()
            assertThat(result?.errorMessage).isEqualTo(testErrorMessage)
        }

    @Ignore
    @Test
    fun `initialize should load metadata from store when available and flow is null`() =
        runTest {
            // Arrange
            val metadataFlow = MutableStateFlow<ImageMetadata?>(testImageMetadata)
            coEvery { imageMetadataStore.imageMetadataFlow } returns metadataFlow

            // Act
            imageUpdateManager.initialize()

            // Assert - verify the flow was updated with store data
            val result = imageUpdateManager.imageUpdateFlow.first()
            assertThat(result).isNotNull()
            assertThat(result).isEqualTo(testImageMetadata)
        }

    @Ignore
    @Test
    fun `initialize should not update flow if existing value is present`() =
        runTest {
            // Arrange
            val metadataFlow = MutableStateFlow<ImageMetadata?>(testImageMetadata)
            coEvery { imageMetadataStore.imageMetadataFlow } returns metadataFlow

            // Pre-set the image update flow
            val existingMetadata =
                ImageMetadata(
                    url = "https://existing.com/image.jpg",
                    refreshIntervalSecs = 300L,
                    errorMessage = null,
                )
            imageUpdateManager.updateImage(
                imageUrl = existingMetadata.url,
                refreshIntervalSecs = existingMetadata.refreshIntervalSecs,
            )

            // Act
            imageUpdateManager.initialize()

            // Assert - flow should still contain the existing value, not the one from the store
            val result = imageUpdateManager.imageUpdateFlow.first()
            assertThat(result?.url).isEqualTo(existingMetadata.url)
            assertThat(result).isNotEqualTo(testImageMetadata)
        }

    @Ignore
    @Test
    fun `initialize should not update flow if store returns null`() =
        runTest {
            // Arrange
            val metadataFlow = MutableStateFlow<ImageMetadata?>(null)
            coEvery { imageMetadataStore.imageMetadataFlow } returns metadataFlow

            // Act
            imageUpdateManager.initialize()

            // Assert - flow should remain null
            val result = imageUpdateManager.imageUpdateFlow.first()
            assertThat(result).isNull()
        }

    @Test
    fun `updateImage should create new metadata instance for each call`() =
        runTest {
            // Act - Call updateImage multiple times
            imageUpdateManager.updateImage(
                imageUrl = "https://first.com/image.png",
                refreshIntervalSecs = 100L,
            )

            val firstResult = imageUpdateManager.imageUpdateFlow.first()

            imageUpdateManager.updateImage(
                imageUrl = "https://second.com/image.png",
                refreshIntervalSecs = 200L,
            )

            val secondResult = imageUpdateManager.imageUpdateFlow.first()

            // Assert - Each result should be a new instance with different values
            assertThat(firstResult).isNotEqualTo(secondResult)
            assertThat(firstResult?.url).isNotEqualTo(secondResult?.url)
            assertThat(firstResult?.refreshIntervalSecs).isNotEqualTo(secondResult?.refreshIntervalSecs)
        }
}
