package ink.trmnl.android.data.fake

import ink.trmnl.android.data.DeviceSetupInfo
import ink.trmnl.android.data.ImageMetadataStore
import ink.trmnl.android.data.RepositoryConfigProvider
import ink.trmnl.android.data.TrmnlDisplayInfo
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.util.HTTP_200
import timber.log.Timber

// Contains fake implementations of network communication for local development
// and testing without depending on real servers.

/**
 * Generates fake display info for debugging purposes without wasting an API request.
 *
 * ℹ️ This is only used when [RepositoryConfigProvider.shouldUseFakeData] is true.
 */
internal suspend fun generateFakeTrmnlDisplayInfo(
    imageMetadataStore: ImageMetadataStore,
    apiUsed: String,
): TrmnlDisplayInfo {
    Timber.d("DEBUG: Using mock data for display info")
    val timestampMin = System.currentTimeMillis() / 60_000 // Changes every minute
    val mockImageUrl = "https://picsum.photos/300/200?grayscale&time=$timestampMin&api=$apiUsed"
    val mockRefreshRate = 600L

    // Save mock data to the data store
    imageMetadataStore.saveImageMetadata(mockImageUrl, mockRefreshRate)

    return TrmnlDisplayInfo(
        status = HTTP_200,
        trmnlDeviceType = TrmnlDeviceType.TRMNL,
        imageUrl = mockImageUrl,
        imageFileName = "mocked-image-" + mockImageUrl.substringAfterLast('?'),
        error = null,
        refreshIntervalSeconds = mockRefreshRate,
    )
}

/**
 * Generates fake setup info for debugging purposes without wasting an API request.
 *
 * ℹ️ This is only used when [RepositoryConfigProvider.shouldUseFakeData] is true.
 */
internal fun generateFakeDeviceSetupInfo(): DeviceSetupInfo =
    DeviceSetupInfo(
        success = true,
        deviceMacId = "A1:B2:C3:D4:E5:F6",
        apiKey = "mocked-api-key-${System.currentTimeMillis()}",
        message = "Mocked device setup successful",
    )
