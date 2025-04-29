package dev.hossain.trmnl.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.hossain.trmnl.MainActivity
import dev.hossain.trmnl.data.TrmnlDisplayRepository
import dev.hossain.trmnl.data.TrmnlTokenDataStore
import dev.hossain.trmnl.data.log.TrmnlRefreshLogManager
import dev.hossain.trmnl.di.WorkerModule
import dev.hossain.trmnl.ui.display.TrmnlMirrorDisplayScreen
import dev.hossain.trmnl.util.isHttpError
import dev.hossain.trmnl.util.isHttpOk
import dev.hossain.trmnl.work.RefreshWorkResult.FAILURE
import dev.hossain.trmnl.work.RefreshWorkResult.SUCCESS
import dev.hossain.trmnl.work.TrmnlWorkScheduler.Companion.IMAGE_REFRESH_PERIODIC_WORK_TAG
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
    private val trmnlTokenDataStore: TrmnlTokenDataStore,
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

        // Get current token
        val token = trmnlTokenDataStore.accessTokenFlow.firstOrNull()

        if (token.isNullOrBlank()) {
            Timber.tag(TAG).w("Token is not set, skipping image refresh")
            refreshLogManager.addFailureLog("No access token found")
            return Result.failure(
                workDataOf(
                    KEY_REFRESH_RESULT to FAILURE.name,
                    KEY_ERROR_MESSAGE to "No access token found",
                ),
            )
        }

        // Fetch TRMNL display image - current or next from playlist based on request type
        val trmnlDisplayInfo =
            if (loadNextPluginImage) {
                displayRepository.getNextDisplayData(token)
            } else {
                displayRepository.getCurrentDisplayData(token)
            }

        // Check for errors
        if (trmnlDisplayInfo.status.isHttpError()) {
            Timber.tag(TAG).w("Failed to fetch display data: ${trmnlDisplayInfo.error}")
            refreshLogManager.addFailureLog(trmnlDisplayInfo.error ?: "Unknown server error")
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
            refreshLogManager.addFailureLog("No image URL provided in response. ${trmnlDisplayInfo.error}")
            return Result.failure(
                workDataOf(
                    KEY_REFRESH_RESULT to FAILURE.name,
                    KEY_ERROR_MESSAGE to "No image URL provided in response. ${trmnlDisplayInfo.error}",
                ),
            )
        }

        // ✅ Log success and update image
        refreshLogManager.addSuccessLog(
            imageUrl = trmnlDisplayInfo.imageUrl,
            imageName = trmnlDisplayInfo.imageName,
            refreshIntervalSeconds = trmnlDisplayInfo.refreshIntervalSeconds,
            imageRefreshWorkType = workTypeValue,
        )

        // Check if we should adapt refresh rate
        val refreshRate = trmnlDisplayInfo.refreshIntervalSeconds
        refreshRate?.let { newRefreshRateSec ->
            if (trmnlTokenDataStore.shouldUpdateRefreshRate(newRefreshRateSec)) {
                Timber.tag(TAG).d("Refresh rate changed, updating periodic work and saving new rate")
                trmnlTokenDataStore.saveRefreshRateSeconds(newRefreshRateSec)
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
            private val trmnlTokenDataStore: TrmnlTokenDataStore,
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
                    trmnlTokenDataStore = trmnlTokenDataStore,
                    refreshLogManager = refreshLogManager,
                    trmnlWorkScheduler = trmnlWorkScheduler,
                    trmnlImageUpdateManager = trmnlImageUpdateManager,
                )
        }
}
