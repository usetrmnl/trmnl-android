package ink.trmnl.android.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import ink.trmnl.android.data.ImageMetadata
import ink.trmnl.android.data.ImageMetadataStore
import ink.trmnl.android.data.TrmnlDeviceConfigDataStore
import ink.trmnl.android.data.TrmnlDisplayInfo
import ink.trmnl.android.data.TrmnlDisplayRepository
import ink.trmnl.android.data.log.TrmnlRefreshLogManager
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.util.HTTP_200
import ink.trmnl.android.util.HTTP_429
import ink.trmnl.android.util.HTTP_500
import ink.trmnl.android.work.RefreshWorkResult.FAILURE
import ink.trmnl.android.work.RefreshWorkResult.SUCCESS
import ink.trmnl.android.work.TrmnlImageRefreshWorker.Companion.KEY_ERROR_MESSAGE
import ink.trmnl.android.work.TrmnlImageRefreshWorker.Companion.KEY_NEW_IMAGE_URL
import ink.trmnl.android.work.TrmnlImageRefreshWorker.Companion.KEY_REFRESH_RESULT
import ink.trmnl.android.work.TrmnlImageRefreshWorker.Companion.PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE
import ink.trmnl.android.work.TrmnlImageRefreshWorker.Companion.PARAM_REFRESH_WORK_TYPE
import ink.trmnl.android.work.TrmnlWorkScheduler.Companion.IMAGE_REFRESH_PERIODIC_WORK_TAG
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class TrmnlImageRefreshWorkerTest {
    // Mock objects
    private lateinit var context: Context
    private lateinit var workerParameters: WorkerParameters
    private lateinit var displayRepository: TrmnlDisplayRepository
    private lateinit var trmnlDeviceConfigDataStore: TrmnlDeviceConfigDataStore
    private lateinit var refreshLogManager: TrmnlRefreshLogManager
    private lateinit var trmnlWorkScheduler: TrmnlWorkScheduler
    private lateinit var trmnlImageUpdateManager: TrmnlImageUpdateManager
    private lateinit var imageMetadataStore: ImageMetadataStore

    private lateinit var worker: TrmnlImageRefreshWorker

    // Sample data
    private val validImageUrl = "https://trmnl.com/images/valid-image.png"
    private val validRefreshRate = 600L
    private val trmnlDeviceConfig =
        TrmnlDeviceConfig(
            type = TrmnlDeviceType.TRMNL,
            apiBaseUrl = "https://api.trmnl.com",
            apiAccessToken = "valid-token",
            refreshRateSecs = validRefreshRate,
        )
    private val byosDeviceConfig =
        TrmnlDeviceConfig(
            type = TrmnlDeviceType.BYOS,
            apiBaseUrl = "https://custom-server.com",
            apiAccessToken = "valid-token",
            refreshRateSecs = validRefreshRate,
        )
    private val byodDeviceConfig =
        TrmnlDeviceConfig(
            type = TrmnlDeviceType.BYOD,
            apiBaseUrl = "https://api.trmnl.com",
            apiAccessToken = "valid-token",
            refreshRateSecs = validRefreshRate,
        )

    @Before
    fun setUp() {
        // Initialize all mocks
        context = mockk(relaxed = true)
        workerParameters = mockk(relaxed = true)
        displayRepository = mockk(relaxed = true)
        trmnlDeviceConfigDataStore = mockk(relaxed = true)
        refreshLogManager = mockk(relaxed = true)
        trmnlWorkScheduler = mockk(relaxed = true)
        trmnlImageUpdateManager = mockk(relaxed = true)
        imageMetadataStore = mockk(relaxed = true)

        // Create the worker with mocked dependencies
        worker =
            TrmnlImageRefreshWorker(
                appContext = context,
                params = workerParameters,
                displayRepository = displayRepository,
                trmnlDeviceConfigDataStore = trmnlDeviceConfigDataStore,
                refreshLogManager = refreshLogManager,
                trmnlWorkScheduler = trmnlWorkScheduler,
                trmnlImageUpdateManager = trmnlImageUpdateManager,
                imageMetadataStore = imageMetadataStore,
            )

        // Set up default input data for the worker
        every { workerParameters.inputData } returns
            workDataOf(
                PARAM_REFRESH_WORK_TYPE to RefreshWorkType.ONE_TIME.name,
                PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE to false,
            )

        // Setup mock device config flow
        every { trmnlDeviceConfigDataStore.deviceConfigFlow } returns MutableStateFlow(trmnlDeviceConfig)
    }

    @After
    fun tearDown() {
        // No explicit cleanup needed for MockK
    }

    @Test
    fun `doWork returns failure when no device config is found`() =
        runTest {
            // Arrange
            every { trmnlDeviceConfigDataStore.deviceConfigFlow } returns flow { emit(null) }

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
            val outputData = (result as ListenableWorker.Result.Failure).outputData
            assertThat(outputData.getString(KEY_REFRESH_RESULT)).isEqualTo(FAILURE.name)
            assertThat(outputData.getString(KEY_ERROR_MESSAGE)).isEqualTo("No device config with API token found")

            // Verify no interactions with repository
            coVerify(exactly = 0) { displayRepository.getCurrentDisplayData(any()) }
            coVerify(exactly = 0) { displayRepository.getNextDisplayData(any()) }
            coVerify { refreshLogManager.addFailureLog(error = "No device config with API token found") }
        }

    @Test
    fun `doWork uses getCurrentDisplayData for TRMNL device type when not loading next image`() =
        runTest {
            // Arrange
            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(any()) } returns false

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val outputData = (result as ListenableWorker.Result.Success).outputData
            assertThat(outputData.getString(KEY_REFRESH_RESULT)).isEqualTo(SUCCESS.name)
            assertThat(outputData.getString(KEY_NEW_IMAGE_URL)).isEqualTo(validImageUrl)

            // Verify correct API was called
            coVerify { displayRepository.getCurrentDisplayData(any()) }
            coVerify(exactly = 0) { displayRepository.getNextDisplayData(any()) }
            coVerify {
                refreshLogManager.addSuccessLog(
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = validImageUrl,
                    imageName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                    imageRefreshWorkType = RefreshWorkType.ONE_TIME.name,
                    httpResponseMetadata = null,
                )
            }
        }

    @Test
    fun `doWork uses getNextDisplayData for BYOD device type`() =
        runTest {
            // Arrange
            every { trmnlDeviceConfigDataStore.deviceConfigFlow } returns MutableStateFlow(byodDeviceConfig)

            // BYOD defaults to master mode (advances playlist) unless explicitly set otherwise
            every { workerParameters.inputData } returns
                workDataOf(
                    PARAM_REFRESH_WORK_TYPE to RefreshWorkType.ONE_TIME.name,
                    PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE to true,
                )

            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.BYOD,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getNextDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(any()) } returns false

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Verify correct API was called
            coVerify { displayRepository.getNextDisplayData(any()) }
            coVerify(exactly = 0) { displayRepository.getCurrentDisplayData(any()) }
        }

    @Test
    fun `doWork uses getNextDisplayData for BYOS device type`() =
        runTest {
            // Arrange
            every { trmnlDeviceConfigDataStore.deviceConfigFlow } returns MutableStateFlow(byosDeviceConfig)

            // BYOS always advances playlist
            every { workerParameters.inputData } returns
                workDataOf(
                    PARAM_REFRESH_WORK_TYPE to RefreshWorkType.ONE_TIME.name,
                    PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE to true,
                )

            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.BYOS,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getNextDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(any()) } returns false

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Verify correct API was called
            coVerify { displayRepository.getNextDisplayData(any()) }
            coVerify(exactly = 0) { displayRepository.getCurrentDisplayData(any()) }
        }

    @Test
    fun `doWork uses getNextDisplayData when loadNextPlaylistImage parameter is true`() =
        runTest {
            // Arrange
            every { workerParameters.inputData } returns
                workDataOf(
                    PARAM_REFRESH_WORK_TYPE to RefreshWorkType.ONE_TIME.name,
                    PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE to true,
                )

            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getNextDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(any()) } returns false

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Verify correct API was called
            coVerify { displayRepository.getNextDisplayData(any()) }
            coVerify(exactly = 0) { displayRepository.getCurrentDisplayData(any()) }
        }

    @Test
    fun `doWork returns failure on HTTP error`() =
        runTest {
            // Arrange
            val errorResponse =
                TrmnlDisplayInfo(
                    status = HTTP_500,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = "",
                    imageFileName = "",
                    error = "Device not found",
                    refreshIntervalSeconds = null,
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns errorResponse

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
            val outputData = (result as ListenableWorker.Result.Failure).outputData
            assertThat(outputData.getString(KEY_REFRESH_RESULT)).isEqualTo(FAILURE.name)
            assertThat(outputData.getString(KEY_ERROR_MESSAGE)).isEqualTo("Device not found")

            // Verify error logging
            coVerify {
                refreshLogManager.addFailureLog(
                    error = "Device not found",
                    httpResponseMetadata = null,
                )
            }
        }

    @Test
    fun `doWork returns failure when image URL is empty`() =
        runTest {
            // Arrange
            val emptyUrlResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = "",
                    imageFileName = "",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns emptyUrlResponse

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
            val outputData = (result as ListenableWorker.Result.Failure).outputData
            assertThat(outputData.getString(KEY_REFRESH_RESULT)).isEqualTo(FAILURE.name)
            assertThat(outputData.getString(KEY_ERROR_MESSAGE)).contains("No image URL provided")
        }

    @Test
    fun `doWork updates refresh rate when needed`() =
        runTest {
            // Arrange
            val newRefreshRate = 1200L
            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = newRefreshRate,
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(newRefreshRate) } returns true

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Verify refresh rate was updated
            coVerify { trmnlDeviceConfigDataStore.saveRefreshRateSeconds(newRefreshRate) }
            verify { trmnlWorkScheduler.scheduleImageRefreshWork(newRefreshRate) }
        }

    @Test
    fun `doWork updates image for periodic work when tag matches`() =
        runTest {
            // Arrange
            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(any()) } returns false
            every { workerParameters.tags } returns setOf(IMAGE_REFRESH_PERIODIC_WORK_TAG)

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Verify image was updated
            verify {
                trmnlImageUpdateManager.updateImage(
                    validImageUrl,
                    validRefreshRate,
                )
            }
        }

    @Test
    fun `doWork does not update image for non-periodic work`() =
        runTest {
            // Arrange
            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(any()) } returns false
            every { workerParameters.tags } returns setOf("other-tag")

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Verify image was not updated via manager (it's still in the result)
            verify(exactly = 0) { trmnlImageUpdateManager.updateImage(any(), any()) }
        }

    @Test
    fun `doWork uses getNextDisplayData for BYOD master device - Issue 190`() =
        runTest {
            // Arrange - BYOD configured as master device
            val byodMasterConfig =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOD,
                    apiBaseUrl = "https://api.trmnl.com",
                    apiAccessToken = "valid-token",
                    refreshRateSecs = validRefreshRate,
                    isMasterDevice = true,
                )
            every { trmnlDeviceConfigDataStore.deviceConfigFlow } returns MutableStateFlow(byodMasterConfig)

            // Master device should advance playlist
            every { workerParameters.inputData } returns
                workDataOf(
                    PARAM_REFRESH_WORK_TYPE to RefreshWorkType.ONE_TIME.name,
                    PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE to true,
                )

            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.BYOD,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getNextDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(any()) } returns false

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Verify correct API was called (master mode uses getNextDisplayData)
            coVerify { displayRepository.getNextDisplayData(any()) }
            coVerify(exactly = 0) { displayRepository.getCurrentDisplayData(any()) }
        }

    @Test
    fun `doWork uses getCurrentDisplayData for BYOD slave device - Issue 190`() =
        runTest {
            // Arrange - BYOD configured as slave device (mirror mode)
            val byodSlaveConfig =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOD,
                    apiBaseUrl = "https://api.trmnl.com",
                    apiAccessToken = "valid-token",
                    refreshRateSecs = validRefreshRate,
                    isMasterDevice = false,
                )
            every { trmnlDeviceConfigDataStore.deviceConfigFlow } returns MutableStateFlow(byodSlaveConfig)

            // Override the default input data to not advance playlist
            every { workerParameters.inputData } returns
                workDataOf(
                    PARAM_REFRESH_WORK_TYPE to RefreshWorkType.ONE_TIME.name,
                    PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE to false,
                )

            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.BYOD,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(any()) } returns false

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Verify correct API was called (slave mode uses getCurrentDisplayData)
            coVerify { displayRepository.getCurrentDisplayData(any()) }
            coVerify(exactly = 0) { displayRepository.getNextDisplayData(any()) }
        }

    @Test
    fun `doWork defaults to getNextDisplayData for BYOD when isMasterDevice is null - backward compatibility - Issue 190`() =
        runTest {
            // Arrange - BYOD without isMasterDevice setting (legacy config)
            val byodLegacyConfig =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOD,
                    apiBaseUrl = "https://api.trmnl.com",
                    apiAccessToken = "valid-token",
                    refreshRateSecs = validRefreshRate,
                    isMasterDevice = null, // Not set - should default to master mode
                )
            every { trmnlDeviceConfigDataStore.deviceConfigFlow } returns MutableStateFlow(byodLegacyConfig)

            // Legacy config should default to master mode (advance playlist)
            // The scheduler sets this based on isMasterDevice ?? true
            every { workerParameters.inputData } returns
                workDataOf(
                    PARAM_REFRESH_WORK_TYPE to RefreshWorkType.ONE_TIME.name,
                    PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE to true,
                )

            val successfulResponse =
                TrmnlDisplayInfo(
                    status = HTTP_200,
                    trmnlDeviceType = TrmnlDeviceType.BYOD,
                    imageUrl = validImageUrl,
                    imageFileName = "test-image.png",
                    refreshIntervalSeconds = validRefreshRate,
                )

            coEvery { displayRepository.getNextDisplayData(any()) } returns successfulResponse
            coEvery { trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(any()) } returns false

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Verify defaults to master mode behavior (uses getNextDisplayData)
            coVerify { displayRepository.getNextDisplayData(any()) }
            coVerify(exactly = 0) { displayRepository.getCurrentDisplayData(any()) }
        }

    @Test
    fun `doWork returns retry and shows cached image when HTTP 429 rate limit error occurs`() =
        runTest {
            // Arrange - Set up cached image metadata
            val cachedImageUrl = "https://test.com/cached-image.png"
            val cachedRefreshRate = 600L
            val cachedMetadata =
                ImageMetadata(
                    url = cachedImageUrl,
                    refreshIntervalSecs = cachedRefreshRate,
                    errorMessage = null,
                    httpStatusCode = null,
                )

            every { imageMetadataStore.imageMetadataFlow } returns flow { emit(cachedMetadata) }
            coEvery { imageMetadataStore.saveImageMetadata(any(), any(), any()) } returns Unit

            // Set up rate limit error response
            val rateLimitResponse =
                TrmnlDisplayInfo(
                    status = HTTP_429,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = "",
                    imageFileName = "",
                    refreshIntervalSeconds = null,
                    error = "Rate limit exceeded",
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns rateLimitResponse

            // Act
            val result = worker.doWork()

            // Assert - Should return retry result
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)

            // Verify cached image metadata was saved with 429 status code
            coVerify {
                imageMetadataStore.saveImageMetadata(
                    imageUrl = cachedImageUrl,
                    refreshIntervalSec = cachedRefreshRate,
                    httpStatusCode = 429,
                )
            }

            // Verify cached image was sent to update manager
            verify {
                trmnlImageUpdateManager.updateImage(
                    imageUrl = cachedImageUrl,
                    refreshIntervalSecs = cachedRefreshRate,
                    errorMessage = null,
                )
            }

            // Verify failure was logged
            coVerify {
                refreshLogManager.addFailureLog(
                    error = "Rate limit exceeded (HTTP 429) - Too many requests. Will retry automatically.",
                    httpResponseMetadata = any(),
                )
            }
        }

    @Test
    fun `doWork returns retry without updating image when HTTP 429 occurs and no cached image exists`() =
        runTest {
            // Arrange - No cached image
            every { imageMetadataStore.imageMetadataFlow } returns flow { emit(null) }

            // Set up rate limit error response
            val rateLimitResponse =
                TrmnlDisplayInfo(
                    status = HTTP_429,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = "",
                    imageFileName = "",
                    refreshIntervalSeconds = null,
                    error = "Rate limit exceeded",
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns rateLimitResponse

            // Act
            val result = worker.doWork()

            // Assert - Should return retry result
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)

            // Verify no attempt to update image (no cached image available)
            verify(exactly = 0) {
                trmnlImageUpdateManager.updateImage(any(), any(), any())
            }

            // Verify no attempt to save metadata (no cached image to save)
            coVerify(exactly = 0) {
                imageMetadataStore.saveImageMetadata(any(), any(), any())
            }

            // Verify failure was still logged
            coVerify {
                refreshLogManager.addFailureLog(
                    error = "Rate limit exceeded (HTTP 429) - Too many requests. Will retry automatically.",
                    httpResponseMetadata = any(),
                )
            }
        }

    @Test
    fun `doWork returns retry when HTTP 429 occurs with blank cached image URL`() =
        runTest {
            // Arrange - Cached metadata exists but URL is blank
            val cachedMetadata =
                ImageMetadata(
                    url = "",
                    refreshIntervalSecs = 600L,
                    errorMessage = null,
                    httpStatusCode = null,
                )

            every { imageMetadataStore.imageMetadataFlow } returns flow { emit(cachedMetadata) }

            // Set up rate limit error response
            val rateLimitResponse =
                TrmnlDisplayInfo(
                    status = HTTP_429,
                    trmnlDeviceType = TrmnlDeviceType.TRMNL,
                    imageUrl = "",
                    imageFileName = "",
                    refreshIntervalSeconds = null,
                    error = "Rate limit exceeded",
                )

            coEvery { displayRepository.getCurrentDisplayData(any()) } returns rateLimitResponse

            // Act
            val result = worker.doWork()

            // Assert - Should return retry result
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)

            // Verify no attempt to update image (blank URL)
            verify(exactly = 0) {
                trmnlImageUpdateManager.updateImage(any(), any(), any())
            }

            // Verify no attempt to save metadata (blank URL)
            coVerify(exactly = 0) {
                imageMetadataStore.saveImageMetadata(any(), any(), any())
            }
        }
}
