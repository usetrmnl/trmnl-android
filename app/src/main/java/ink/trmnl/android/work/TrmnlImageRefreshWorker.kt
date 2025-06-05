package ink.trmnl.android.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ink.trmnl.android.MainActivity
import ink.trmnl.android.data.TrmnlDeviceConfigDataStore
import ink.trmnl.android.data.TrmnlDisplayInfo
import ink.trmnl.android.data.TrmnlDisplayRepository
import ink.trmnl.android.data.log.TrmnlRefreshLogManager
import ink.trmnl.android.di.WorkerModule
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.ui.display.TrmnlMirrorDisplayScreen
import ink.trmnl.android.util.isHttpError
import ink.trmnl.android.util.isHttpOk
import ink.trmnl.android.work.RefreshWorkResult.FAILURE
import ink.trmnl.android.work.RefreshWorkResult.SUCCESS
import ink.trmnl.android.work.TrmnlWorkScheduler.Companion.IMAGE_REFRESH_PERIODIC_WORK_TAG
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import javax.inject.Inject

/**
 * Worker to refresh the image displayed on the TRMNL mirror display.
 *
 * The worker result is observed in [MainActivity] and then updated via the [TrmnlImageUpdateManager].
 * Whenever the image is updated, the [TrmnlImageUpdateManager] will notify the observers.
 * In this case the [TrmnlMirrorDisplayScreen] will recompose and update the image.
 *
 * @see TrmnlImageUpdateManager
 * @see TrmnlMirrorDisplayScreen
 */
class TrmnlImageRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
    private val displayRepository: TrmnlDisplayRepository,
    private val trmnlDeviceConfigDataStore: TrmnlDeviceConfigDataStore,
    private val refreshLogManager: TrmnlRefreshLogManager,
    private val trmnlWorkScheduler: TrmnlWorkScheduler,
    private val trmnlImageUpdateManager: TrmnlImageUpdateManager,
) : CoroutineWorker(appContext, params) {
    companion object {
        private const val TAG = "TrmnlWorker"

        const val KEY_REFRESH_RESULT = "refresh_result"
        const val KEY_NEW_IMAGE_URL = "new_image_url"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val PARAM_REFRESH_WORK_TYPE = "refresh_work_type"
        const val PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE = "load_next_playlist_image"
    }

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("Starting image refresh work ($tags)")

        // Get the work type from the input data
        val workTypeValue = inputData.getString(PARAM_REFRESH_WORK_TYPE) ?: RefreshWorkType.ONE_TIME.name
        val loadNextPluginImage = inputData.getBoolean(PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE, false)

        Timber.tag(TAG).d("Work type: $workTypeValue, loadNextPluginImage: $loadNextPluginImage")

        // Get device config
        val deviceConfig: TrmnlDeviceConfig? = trmnlDeviceConfigDataStore.deviceConfigFlow.firstOrNull()

        if (deviceConfig == null) {
            Timber.tag(TAG).w("Device config and token is not set, skipping image refresh")
            refreshLogManager.addFailureLog("No device config with API token found")
            return Result.failure(
                workDataOf(
                    KEY_REFRESH_RESULT to FAILURE.name,
                    KEY_ERROR_MESSAGE to "No device config with API token found",
                ),
            )
        }

        // Fetch TRMNL display image - current or next from playlist based on request type
        val trmnlDisplayInfo: TrmnlDisplayInfo =
            if (loadNextPluginImage) {
                displayRepository.getNextDisplayData(deviceConfig)
            } else {
                displayRepository.getCurrentDisplayData(deviceConfig)
            }

        // Check for errors
        if (trmnlDisplayInfo.status.isHttpError()) {
            Timber.tag(TAG).w("Failed to fetch display data: ${trmnlDisplayInfo.error}")
            refreshLogManager.addFailureLog(
                error = trmnlDisplayInfo.error ?: "Unknown server error",
                httpResponseMetadata = trmnlDisplayInfo.httpResponseMetadata,
            )
            return Result.failure(
                workDataOf(
                    KEY_REFRESH_RESULT to FAILURE.name,
                    KEY_ERROR_MESSAGE to (trmnlDisplayInfo.error ?: "Unknown server error"),
                ),
            )
        }

        // Check if image URL is valid
        if (trmnlDisplayInfo.imageUrl.isEmpty() || trmnlDisplayInfo.status.isHttpOk().not()) {
            Timber.tag(TAG).w("No image URL provided in response. ${trmnlDisplayInfo.error}")
            refreshLogManager.addFailureLog(
                error = "No image URL provided in response. ${trmnlDisplayInfo.error}",
                httpResponseMetadata = trmnlDisplayInfo.httpResponseMetadata,
            )
            return Result.failure(
                workDataOf(
                    KEY_REFRESH_RESULT to FAILURE.name,
                    KEY_ERROR_MESSAGE to "No image URL provided in response. ${trmnlDisplayInfo.error}",
                ),
            )
        }

        // ✅ Log success and update image
        refreshLogManager.addSuccessLog(
            trmnlDeviceType = deviceConfig.type,
            imageUrl = trmnlDisplayInfo.imageUrl,
            imageName = trmnlDisplayInfo.imageName,
            refreshIntervalSeconds = trmnlDisplayInfo.refreshIntervalSeconds,
            imageRefreshWorkType = workTypeValue,
            httpResponseMetadata = trmnlDisplayInfo.httpResponseMetadata,
        )

        // Check if we should adapt refresh rate
        val refreshRate = trmnlDisplayInfo.refreshIntervalSeconds
        refreshRate?.let { newRefreshRateSec ->
            if (trmnlDeviceConfigDataStore.shouldUpdateRefreshRate(newRefreshRateSec)) {
                Timber.tag(TAG).d("Refresh rate changed, updating periodic work and saving new rate")
                trmnlDeviceConfigDataStore.saveRefreshRateSeconds(newRefreshRateSec)
                trmnlWorkScheduler.scheduleImageRefreshWork(newRefreshRateSec)
            } else {
                Timber.tag(TAG).d("Refresh rate is unchanged, not updating")
            }
        }

        // Workaround for periodic work not updating correctly (might be because of 🐛 bug in library)
        conditionallyUpdateImageForPeriodicWork(tags, trmnlDisplayInfo.imageUrl, trmnlDisplayInfo.refreshIntervalSeconds)

        Timber.tag(TAG).i("Image refresh successful for work($tags), got new URL: ${trmnlDisplayInfo.imageUrl}")
        return Result.success(
            workDataOf(
                KEY_REFRESH_RESULT to SUCCESS.name,
                KEY_NEW_IMAGE_URL to trmnlDisplayInfo.imageUrl,
            ),
        )
    }

    /**
     * There is potentially a bug where periodic work is not updated correctly.
     * This function will check if the image URL is different from the one in the store.
     *
     * https://stackoverflow.com/questions/51476480/workstatus-observer-always-in-enqueued-state
     */
    private fun conditionallyUpdateImageForPeriodicWork(
        tags: Set<String>,
        imageUrl: String,
        refreshIntervalSecs: Long?,
    ) {
        if (tags.contains(IMAGE_REFRESH_PERIODIC_WORK_TAG)) {
            Timber.tag(TAG).d("Periodic work detected, updating image URL from result")
            trmnlImageUpdateManager.updateImage(imageUrl, refreshIntervalSecs)
        }
    }

    /**
     * Factory class for creating instances of [TrmnlImageRefreshWorker] with additional dependency using DI.
     *
     * @see TrmnlWorkerFactory
     * @see WorkerModule
     */
    class Factory
        @Inject
        constructor(
            private val displayRepository: TrmnlDisplayRepository,
            private val trmnlDeviceConfigDataStore: TrmnlDeviceConfigDataStore,
            private val refreshLogManager: TrmnlRefreshLogManager,
            private val trmnlWorkScheduler: TrmnlWorkScheduler,
            private val trmnlImageUpdateManager: TrmnlImageUpdateManager,
        ) {
            fun create(
                appContext: Context,
                params: WorkerParameters,
            ): TrmnlImageRefreshWorker =
                TrmnlImageRefreshWorker(
                    appContext = appContext,
                    params = params,
                    displayRepository = displayRepository,
                    trmnlDeviceConfigDataStore = trmnlDeviceConfigDataStore,
                    refreshLogManager = refreshLogManager,
                    trmnlWorkScheduler = trmnlWorkScheduler,
                    trmnlImageUpdateManager = trmnlImageUpdateManager,
                )
        }
}
