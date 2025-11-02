package ink.trmnl.android.work

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.squareup.anvil.annotations.optional.SingleIn
import ink.trmnl.android.data.TrmnlDeviceConfigDataStore
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.di.ApplicationContext
import ink.trmnl.android.work.TrmnlImageRefreshWorker.Companion.PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE
import ink.trmnl.android.work.TrmnlImageRefreshWorker.Companion.PARAM_REFRESH_WORK_TYPE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Manages the scheduling and execution of background work using WorkManager.
 * This includes scheduling periodic image refresh work and handling one-time work requests.
 */
@SingleIn(AppScope::class)
class TrmnlWorkScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val trmnlDeviceConfigDataStore: TrmnlDeviceConfigDataStore,
    ) {
        companion object {
            internal const val IMAGE_REFRESH_PERIODIC_WORK_NAME = "trmnl_image_refresh_work_periodic"
            internal const val IMAGE_REFRESH_PERIODIC_WORK_TAG = "trmnl_image_refresh_work_periodic_tag"
            internal const val IMAGE_REFRESH_ONETIME_WORK_NAME = "trmnl_image_refresh_work_onetime"
            internal const val IMAGE_REFRESH_ONETIME_WORK_TAG = "trmnl_image_refresh_work_onetime_tag"

            /**
             * Minimum interval for periodic work in minutes.
             * This is the minimum interval required by WorkManager for periodic work.
             *
             * - https://developer.android.com/reference/androidx/work/PeriodicWorkRequest
             * - https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
             */
            private const val WORK_MANAGER_MINIMUM_INTERVAL_MINUTES = 15L

            /**
             * When loading current image of the TRMNL, we add this delay before fetching
             * the image allowing the server to render the image and save it in cloud.
             */
            private const val EXTRA_REFRESH_WAIT_TIME_SEC: Long = 60L // 60 seconds
        }

        /**
         * Schedule periodic image refresh work with device-type-specific behavior.
         *
         * This method:
         * - Adds 60 seconds buffer time to the interval before scheduling
         * - Converts the interval to minutes with a minimum of 15 minutes (WorkManager requirement)
         * - Requires a valid token to be set, otherwise scheduling is skipped
         * - Updates existing scheduled work if already present
         * - For BYOS devices: Advances through the playlist automatically (playlist cycling)
         * - For TRMNL devices: Mirrors the current display from the official TRMNL device
         * - Requires network connectivity
         * - Uses exponential backoff for retries
         *
         * @param intervalSeconds The desired refresh interval in seconds (will be adjusted)
         */
        fun scheduleImageRefreshWork(intervalSeconds: Long) {
            // Check if we already have work scheduled
            val workInfos =
                WorkManager
                    .getInstance(context)
                    .getWorkInfosForUniqueWork(IMAGE_REFRESH_PERIODIC_WORK_NAME)
                    .get()

            val existingWork = workInfos.firstOrNull()
            if (existingWork != null) {
                Timber.d("Existing work found: ${existingWork.state}")

                // Optional: Get the existing work details
                val nextScheduleTimeMillis = existingWork.nextScheduleTimeMillis
                val nextScheduleTime = java.time.Instant.ofEpochMilli(nextScheduleTimeMillis)
                Timber.d("Next schedule time: $nextScheduleTimeMillis ($nextScheduleTime)")
            } else {
                Timber.d("No existing work found, will create new work")
            }

            // Add extra wait time to interval and convert seconds to minutes
            val adjustedIntervalSeconds = intervalSeconds + EXTRA_REFRESH_WAIT_TIME_SEC
            val intervalMinutes = (adjustedIntervalSeconds / 60).coerceAtLeast(WORK_MANAGER_MINIMUM_INTERVAL_MINUTES)

            Timber.d("Scheduling work: $intervalSeconds seconds + $EXTRA_REFRESH_WAIT_TIME_SEC seconds → $intervalMinutes minutes")

            if (trmnlDeviceConfigDataStore.hasTokenSync().not()) {
                Timber.w("Token not set, skipping image refresh work scheduling")
                return
            }

            // Determine whether to advance playlist based on device type
            // - BYOS devices always advance their own playlist
            // - BYOD devices can be configured as master (advance) or slave (mirror) via isMasterDevice setting
            // - TRMNL devices always mirror the official TRMNL device (stay on current screen)
            // See https://github.com/usetrmnl/trmnl-android/issues/190
            val deviceConfig = trmnlDeviceConfigDataStore.getDeviceConfigSync()
            val shouldAdvancePlaylist =
                when (deviceConfig?.type) {
                    ink.trmnl.android.model.TrmnlDeviceType.BYOS -> true // Always auto-advance
                    ink.trmnl.android.model.TrmnlDeviceType.BYOD -> deviceConfig.isMasterDevice ?: true // Default to master if not set
                    ink.trmnl.android.model.TrmnlDeviceType.TRMNL -> false // Always mirror
                    null -> false
                }

            Timber.d(
                "Device type: ${deviceConfig?.type}, isMasterDevice: ${deviceConfig?.isMasterDevice}, shouldAdvancePlaylist: $shouldAdvancePlaylist",
            )

            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val periodicWorkRequest =
                PeriodicWorkRequestBuilder<TrmnlImageRefreshWorker>(
                    repeatInterval = intervalMinutes,
                    repeatIntervalTimeUnit = TimeUnit.MINUTES,
                ).setConstraints(constraints)
                    .setBackoffCriteria(
                        // Exponential backoff for retrying failed work
                        // To avoid overwhelming the server with requests
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                        TimeUnit.MILLISECONDS,
                    ).setInputData(
                        workDataOf(
                            PARAM_REFRESH_WORK_TYPE to RefreshWorkType.PERIODIC.name,
                            // For BYOS devices: advance the playlist (use /api/display endpoint)
                            // For TRMNL devices: mirror the current display (use /api/current_screen endpoint)
                            // This enables automatic playlist cycling for BYOS while maintaining
                            // mirror functionality for TRMNL devices.
                            PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE to shouldAdvancePlaylist,
                        ),
                    ).addTag(IMAGE_REFRESH_PERIODIC_WORK_TAG)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                uniqueWorkName = IMAGE_REFRESH_PERIODIC_WORK_NAME,
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
                request = periodicWorkRequest,
            )
        }

        /**
         * Start a one-time image refresh work immediately.
         *
         * This method:
         * - Executes immediately (subject to network constraints)
         * - Requires a valid token to be set, otherwise work is skipped
         * - Replaces any existing one-time work request
         * - Requires network connectivity
         * - Uses exponential backoff for retries
         *
         * @param loadNextPlaylistImage If true, advances to next playlist item using /api/display endpoint.
         *                              If false, reloads current screen using /api/current_screen endpoint.
         *                              Defaults to false.
         */
        fun startOneTimeImageRefreshWork(loadNextPlaylistImage: Boolean = false) {
            Timber.d("Starting one-time image refresh work with loadNextPlaylistImage: $loadNextPlaylistImage")

            if (trmnlDeviceConfigDataStore.hasTokenSync().not()) {
                Timber.w("Token not set, skipping one-time image refresh work")
                return
            }

            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val workRequest =
                OneTimeWorkRequestBuilder<TrmnlImageRefreshWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                        TimeUnit.MILLISECONDS,
                    ).setInputData(
                        workDataOf(
                            PARAM_REFRESH_WORK_TYPE to RefreshWorkType.ONE_TIME.name,
                            PARAM_LOAD_NEXT_PLAYLIST_DISPLAY_IMAGE to loadNextPlaylistImage,
                        ),
                    ).addTag(IMAGE_REFRESH_ONETIME_WORK_TAG)
                    .build()

            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                    uniqueWorkName = IMAGE_REFRESH_ONETIME_WORK_NAME,
                    existingWorkPolicy = ExistingWorkPolicy.REPLACE,
                    request = workRequest,
                )
        }

        /**
         * Cancel the scheduled periodic image refresh work.
         *
         * Note: This only cancels periodic work, not one-time work requests.
         */
        fun cancelPeriodicImageRefreshWork() {
            WorkManager.getInstance(context).cancelUniqueWork(IMAGE_REFRESH_PERIODIC_WORK_NAME)
        }

        /**
         * Checks if the image refresh work is already scheduled
         * @return Flow of Boolean that emits true if work is scheduled
         */
        fun isImageRefreshWorkScheduled(): Flow<Boolean> {
            val workQuery =
                WorkQuery.Builder
                    .fromUniqueWorkNames(listOf(IMAGE_REFRESH_PERIODIC_WORK_NAME))
                    .addStates(listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED))
                    .build()

            return WorkManager
                .getInstance(context)
                .getWorkInfosLiveData(workQuery)
                .asFlow()
                .map { workInfoList -> workInfoList.isNotEmpty() }
        }

        /**
         * Synchronously checks if image refresh work is scheduled
         * @return true if work is scheduled
         */
        fun isImageRefreshWorkScheduledSync(): Boolean {
            val workInfos: List<WorkInfo> =
                WorkManager
                    .getInstance(context)
                    .getWorkInfosForUniqueWork(IMAGE_REFRESH_PERIODIC_WORK_NAME)
                    .get()

            return workInfos.any {
                it.state == WorkInfo.State.RUNNING ||
                    it.state == WorkInfo.State.ENQUEUED ||
                    it.state == WorkInfo.State.BLOCKED
            }
        }

        /**
         * Get the scheduled periodic work info as a Flow to get updates on upcoming refresh job.
         *
         * @return Flow that emits the current WorkInfo for the periodic work, or null if not scheduled
         */
        fun getScheduledWorkInfo(): Flow<WorkInfo?> =
            WorkManager
                .getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(IMAGE_REFRESH_PERIODIC_WORK_NAME)
                .asFlow()
                .map { it.firstOrNull() }

        /**
         * Update the refresh interval for periodic work.
         *
         * This method:
         * - Saves the new interval to the device config data store
         * - Reschedules the periodic work with the new interval
         *
         * @param newIntervalSeconds The new refresh interval in seconds
         */
        suspend fun updateRefreshInterval(newIntervalSeconds: Long) {
            Timber.d("Updating refresh interval to $newIntervalSeconds seconds")

            // Save the refresh rate to TokenManager
            trmnlDeviceConfigDataStore.saveRefreshRateSeconds(newIntervalSeconds)

            // Reschedule with new interval
            scheduleImageRefreshWork(newIntervalSeconds)
        }
    }
