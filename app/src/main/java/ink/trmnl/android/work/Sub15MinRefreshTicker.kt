package ink.trmnl.android.work

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ink.trmnl.android.data.ImageMetadataStore
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.work.TrmnlWorkScheduler.Companion.WORK_MANAGER_MINIMUM_INTERVAL_SECONDS
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * Isolated hybrid refresh component for sub-15-minute refresh intervals (`1..899` seconds).
 *
 * Android's [ androidx.work.WorkManager ] enforces a strict 15-minute minimum interval for
 * background periodic work (`PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS`).
 *
 * - **When `intervalSeconds >= 15 minutes` (`>= 900s`)**: This component stays **deactivated**
 *   (no-op), and standard `WorkManager` periodic scheduling handles all refreshes.
 * - **When `intervalSeconds < 15 minutes` (`1..899s`) and the display screen is active (`RESUMED`)**:
 *   This component runs a lifecycle-scoped ticker that counts down to the next refresh and triggers
 *   [TrmnlWorkScheduler.triggerScheduledRefresh], coordinating with background `WorkManager` via
 *   [ImageMetadataStore] timestamps and running-work checks to prevent duplicate refreshes.
 *
 * See https://github.com/usetrmnl/trmnl-android/issues/320
 */
@Inject
@SingleIn(AppScope::class)
class Sub15MinRefreshTicker(
    private val imageMetadataStore: ImageMetadataStore,
    private val trmnlWorkScheduler: TrmnlWorkScheduler,
) {
    companion object {
        private const val TAG = "Sub15MinTicker"
        internal const val POLL_DELAY_UNDER_MINUTE_MS = 1_000L
        internal const val POLL_DELAY_DEFAULT_MS = 5_000L
    }

    /**
     * Returns `true` only when [intervalSeconds] is a valid positive interval strictly below
     * WorkManager's 15-minute (900s) minimum.
     */
    fun isSub15MinInterval(intervalSeconds: Long?): Boolean =
        intervalSeconds != null && intervalSeconds in 1 until WORK_MANAGER_MINIMUM_INTERVAL_SECONDS

    /**
     * Runs the sub-15-minute refresh ticker scoped to [Lifecycle.State.RESUMED].
     *
     * When the screen turns off or the app is backgrounded, the underlying coroutine is automatically
     * cancelled and stops ticking. When the screen resumes, it checks elapsed time and catches up
     * immediately if the cached image is stale.
     *
     * If [intervalSeconds] is `>= 15 minutes` (or `<= 0`), this function returns immediately as a no-op.
     */
    suspend fun runWhenResumed(
        lifecycle: Lifecycle?,
        intervalSeconds: Long,
        isLoading: () -> Boolean,
        onRefreshTriggered: () -> Unit,
        onNextRefreshTimeUpdate: (String) -> Unit,
    ) {
        if (!isSub15MinInterval(intervalSeconds)) {
            Timber.tag(TAG).d("Interval ${intervalSeconds}s is >= 15m; Sub15MinRefreshTicker remains deactivated.")
            return
        }

        val collectBlock: suspend () -> Unit = {
            observeTicks(
                intervalSeconds = intervalSeconds,
                isLoading = isLoading,
                onRefreshTriggered = onRefreshTriggered,
            ).collect { countdownText ->
                onNextRefreshTimeUpdate(countdownText)
            }
        }

        if (lifecycle != null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                Timber.tag(TAG).i("Display screen RESUMED with sub-15m interval (${intervalSeconds}s); activating ticker.")
                try {
                    collectBlock()
                } finally {
                    Timber.tag(TAG).i("Display screen left RESUMED state; deactivating sub-15m ticker.")
                }
            }
        } else {
            collectBlock()
        }
    }

    /**
     * Emits formatted countdown strings (`"in 4 minutes"`, `"in 30 seconds"`, `"any moment now"`)
     * and triggers a scheduled refresh when [intervalSeconds] elapses.
     *
     * Coordinates with background `WorkManager` by:
     * 1. Observing [ImageMetadataStore.imageMetadataFlow] so any background periodic refresh immediately
     *    resets the countdown timer.
     * 2. Checking [isLoading] and [TrmnlWorkScheduler.isImageRefreshWorkRunningSync] before triggering.
     */
    internal fun observeTicks(
        intervalSeconds: Long,
        isLoading: () -> Boolean,
        onRefreshTriggered: () -> Unit,
        clock: () -> Long = { System.currentTimeMillis() },
    ): Flow<String> =
        channelFlow {
            if (!isSub15MinInterval(intervalSeconds)) {
                Timber.tag(TAG).d("Interval ${intervalSeconds}s is not < 15m; closing ticker flow.")
                return@channelFlow
            }

            val initialMetadata = imageMetadataStore.imageMetadataFlow.firstOrNull()
            val lastTimestampRef = AtomicReference<Long?>(initialMetadata?.timestamp)

            coroutineScope {
                val metadataJob =
                    launch {
                        imageMetadataStore.imageMetadataFlow.collect { metadata ->
                            if (metadata != null) {
                                lastTimestampRef.set(metadata.timestamp)
                                Timber.tag(TAG).d("Updated last image timestamp from store: ${metadata.timestamp}")
                            }
                        }
                    }

                var lastLoggedRemainingSec = -1L

                while (isActive) {
                    val now = clock()
                    val lastTimestamp = lastTimestampRef.get() ?: now
                    val nextRefreshTimeMillis = lastTimestamp + (intervalSeconds * 1000L)
                    val remainingMillis = nextRefreshTimeMillis - now

                    if (remainingMillis <= 0) {
                        val workAlreadyRunning = isLoading() || trmnlWorkScheduler.isImageRefreshWorkRunningSync()
                        if (!workAlreadyRunning) {
                            Timber.tag(TAG).i(
                                "Sub-15m interval elapsed (${intervalSeconds}s). Triggering scheduled refresh.",
                            )
                            onRefreshTriggered()
                            trmnlWorkScheduler.triggerScheduledRefresh()
                        } else {
                            Timber.tag(TAG).d(
                                "Sub-15m interval elapsed (${intervalSeconds}s), but refresh work is already running. " +
                                    "Skipping duplicate trigger.",
                            )
                        }
                        send("any moment now")
                        delay(POLL_DELAY_DEFAULT_MS)
                    } else {
                        val remainingSeconds = remainingMillis / 1000L
                        val countdownText = formatCountdown(remainingMillis)
                        send(countdownText)

                        val shouldLog =
                            lastLoggedRemainingSec == -1L ||
                                (remainingSeconds < 60 && (remainingSeconds % 10L == 0L || remainingSeconds <= 5L)) ||
                                (remainingSeconds >= 60 && remainingSeconds / 60 != lastLoggedRemainingSec / 60)
                        if (shouldLog) {
                            Timber.tag(TAG).d("Sub-15m ticker countdown: $countdownText (${remainingSeconds}s remaining)")
                            lastLoggedRemainingSec = remainingSeconds
                        }

                        val delayMs =
                            if (remainingSeconds < 60) {
                                POLL_DELAY_UNDER_MINUTE_MS
                            } else {
                                POLL_DELAY_DEFAULT_MS
                            }
                        delay(delayMs)
                    }
                }

                metadataJob.cancel()
            }
        }

    internal fun formatCountdown(remainingMillis: Long): String =
        when {
            remainingMillis <= 0 -> "any moment now"
            remainingMillis < 60_000L -> "in ${remainingMillis / 1_000L} seconds"
            remainingMillis < 3_600_000L -> "in ${remainingMillis / 60_000L} minutes"
            else -> "in ${remainingMillis / 3_600_000L} hours"
        }
}
