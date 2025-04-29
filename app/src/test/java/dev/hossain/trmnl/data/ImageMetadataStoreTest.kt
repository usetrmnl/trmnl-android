package dev.hossain.trmnl.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Unit tests for [ImageMetadataStore].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ImageMetadataStoreTest {
    private lateinit var context: Context
    private lateinit var imageMetadataStore: ImageMetadataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        imageMetadataStore = ImageMetadataStore(context)
    }

    @After
    fun tearDown() =
        runTest {
            // Clean up the data store after each test
            imageMetadataStore.clearImageMetadata()
        }

    @Test
    fun `imageMetadataFlow - given no metadata saved - returns null`() =
        runTest {
            // Act
            val metadata = imageMetadataStore.imageMetadataFlow.first()

            // Assert
            assertThat(metadata).isNull()
        }

    @Test
    fun `imageMetadataFlow - given metadata saved - returns correct metadata`() =
        runTest {
            // Arrange
            val testUrl = "https://test.com/image.png"
            val testRefreshRate = 300L
            imageMetadataStore.saveImageMetadata(testUrl, testRefreshRate)

            // Act
            val metadata = imageMetadataStore.imageMetadataFlow.first()

            // Assert
            assertThat(metadata).isNotNull()
            assertThat(metadata?.url).isEqualTo(testUrl)
            assertThat(metadata?.refreshIntervalSecs).isEqualTo(testRefreshRate)
            assertThat(metadata?.errorMessage).isNull()
            // We can't test exact timestamp since it's set at save time, but we can verify it's recent
            assertThat(metadata?.timestamp).isAtMost(Instant.now().toEpochMilli())
            assertThat(metadata?.timestamp).isAtLeast(Instant.now().minusSeconds(10).toEpochMilli())
        }

    @Test
    fun `hasValidImageUrlFlow - given no metadata saved - returns false`() =
        runTest {
            // Act
            val hasValidUrl = imageMetadataStore.hasValidImageUrlFlow.first()

            // Assert
            assertThat(hasValidUrl).isFalse()
        }

    @Test
    fun `hasValidImageUrlFlow - given metadata saved without refresh rate - returns false`() =
        runTest {
            // Arrange - Save URL without refresh rate
            imageMetadataStore.saveImageMetadata("https://test.com/image.png")

            // Act
            val hasValidUrl = imageMetadataStore.hasValidImageUrlFlow.first()

            // Assert
            assertThat(hasValidUrl).isFalse()
        }

    @Test
    fun `hasValidImageUrlFlow - given valid non-expired metadata - returns true`() =
        runTest {
            // Arrange - Save URL with refresh rate that won't expire soon
            imageMetadataStore.saveImageMetadata("https://test.com/image.png", 3600L)

            // Act
            val hasValidUrl = imageMetadataStore.hasValidImageUrlFlow.first()

            // Assert
            assertThat(hasValidUrl).isTrue()
        }

    @Test
    fun `hasValidImageUrlSync - given valid non-expired metadata - returns true`() =
        runTest {
            // Arrange - Save URL with refresh rate that won't expire soon
            imageMetadataStore.saveImageMetadata("https://test.com/image.png", 3600L)

            // Act
            val hasValidUrl = imageMetadataStore.hasValidImageUrlSync()

            // Assert
            assertThat(hasValidUrl).isTrue()
        }

    @Test
    fun `hasValidImageUrlSync - given no metadata saved - returns false`() =
        runTest {
            // Act
            val hasValidUrl = imageMetadataStore.hasValidImageUrlSync()

            // Assert
            assertThat(hasValidUrl).isFalse()
        }

    @Test
    fun `timeUntilExpirationFlow - given no metadata saved - returns null`() =
        runTest {
            // Act
            val timeUntilExpiration = imageMetadataStore.timeUntilExpirationFlow.first()

            // Assert
            assertThat(timeUntilExpiration).isNull()
        }

    @Test
    fun `timeUntilExpirationFlow - given valid non-expired metadata - returns positive value`() =
        runTest {
            // Arrange - Save URL with refresh rate that won't expire soon
            imageMetadataStore.saveImageMetadata("https://test.com/image.png", 3600L)

            // Act
            val timeUntilExpiration = imageMetadataStore.timeUntilExpirationFlow.first()

            // Assert
            assertThat(timeUntilExpiration).isNotNull()
            assertThat(timeUntilExpiration).isGreaterThan(0L)
            // The time until expiration should be close to 3600 seconds (in milliseconds)
            assertThat(timeUntilExpiration).isAtLeast(3590 * 1000L)
            assertThat(timeUntilExpiration).isAtMost(3600 * 1000L)
        }

    @Test
    fun `clearImageMetadata - given metadata exists - clears all metadata`() =
        runTest {
            // Arrange - Save some metadata
            imageMetadataStore.saveImageMetadata("https://test.com/image.png", 300L)
            assertThat(imageMetadataStore.imageMetadataFlow.first()).isNotNull()

            // Act
            imageMetadataStore.clearImageMetadata()

            // Assert
            assertThat(imageMetadataStore.imageMetadataFlow.first()).isNull()
            assertThat(imageMetadataStore.hasValidImageUrlFlow.first()).isFalse()
            assertThat(imageMetadataStore.timeUntilExpirationFlow.first()).isNull()
        }
}
