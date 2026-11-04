package ink.trmnl.android.work

import com.google.common.truth.Truth.assertThat
import ink.trmnl.android.data.ImageMetadata
import ink.trmnl.android.data.ImageMetadataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Sub15MinRefreshTickerTest {
    private lateinit var imageMetadataStore: ImageMetadataStore
    private lateinit var trmnlWorkScheduler: TrmnlWorkScheduler
    private lateinit var ticker: Sub15MinRefreshTicker

    @Before
    fun setUp() {
        imageMetadataStore = mockk(relaxed = true)
        trmnlWorkScheduler = mockk(relaxed = true)
        coEvery { trmnlWorkScheduler.isImageRefreshWorkRunning() } returns false
        ticker = Sub15MinRefreshTicker(imageMetadataStore, trmnlWorkScheduler)
    }

    @Test
    fun `isSub15MinInterval returns true only for positive intervals under 900 seconds`() {
        assertThat(ticker.isSub15MinInterval(1L)).isTrue()
        assertThat(ticker.isSub15MinInterval(60L)).isTrue()
        assertThat(ticker.isSub15MinInterval(300L)).isTrue()
        assertThat(ticker.isSub15MinInterval(899L)).isTrue()

        // 15 minutes (900s) and above must be false (deactivated)
        assertThat(ticker.isSub15MinInterval(900L)).isFalse()
        assertThat(ticker.isSub15MinInterval(1800L)).isFalse()
        assertThat(ticker.isSub15MinInterval(3600L)).isFalse()

        // Invalid / null intervals must be false
        assertThat(ticker.isSub15MinInterval(0L)).isFalse()
        assertThat(ticker.isSub15MinInterval(-60L)).isFalse()
        assertThat(ticker.isSub15MinInterval(null)).isFalse()
    }

    @Test
    fun `observeTicks stays deactivated and emits nothing when interval is 15 minutes or above`() =
        runTest {
            var refreshTriggered = false
            val emissions =
                ticker
                    .observeTicks(
                        intervalSeconds = 900L,
                        isLoading = { false },
                        onRefreshTriggered = { refreshTriggered = true },
                    ).toList()

            assertThat(emissions).isEmpty()
            assertThat(refreshTriggered).isFalse()
            coVerify(exactly = 0) { trmnlWorkScheduler.triggerScheduledRefresh() }
        }

    @Test
    fun `observeTicks triggers immediate refresh when cached image is stale`() =
        runTest {
            val now = 1_000_000L
            val staleMetadata =
                ImageMetadata(
                    url = "https://trmnl.com/image.png",
                    refreshIntervalSecs = 300L,
                    timestamp = now - 350_000L, // 350s old (> 300s interval)
                )
            every { imageMetadataStore.imageMetadataFlow } returns MutableStateFlow(staleMetadata)

            var refreshTriggered = false
            val firstEmission =
                ticker
                    .observeTicks(
                        intervalSeconds = 300L,
                        isLoading = { false },
                        onRefreshTriggered = { refreshTriggered = true },
                        clock = { now },
                    ).first()

            assertThat(firstEmission).isEqualTo("any moment now")
            assertThat(refreshTriggered).isTrue()
            coVerify(exactly = 1) { trmnlWorkScheduler.triggerScheduledRefresh() }
        }

    @Test
    fun `observeTicks skips refresh trigger when WorkManager refresh work is already running`() =
        runTest {
            val now = 1_000_000L
            val staleMetadata =
                ImageMetadata(
                    url = "https://trmnl.com/image.png",
                    refreshIntervalSecs = 300L,
                    timestamp = now - 350_000L,
                )
            every { imageMetadataStore.imageMetadataFlow } returns MutableStateFlow(staleMetadata)
            coEvery { trmnlWorkScheduler.isImageRefreshWorkRunning() } returns true

            var refreshTriggered = false
            val firstEmission =
                ticker
                    .observeTicks(
                        intervalSeconds = 300L,
                        isLoading = { false },
                        onRefreshTriggered = { refreshTriggered = true },
                        clock = { now },
                    ).first()

            assertThat(firstEmission).isEqualTo("any moment now")
            assertThat(refreshTriggered).isFalse()
            coVerify(exactly = 0) { trmnlWorkScheduler.triggerScheduledRefresh() }
        }

    @Test
    fun `observeTicks skips refresh trigger when UI isLoading is already true`() =
        runTest {
            val now = 1_000_000L
            val staleMetadata =
                ImageMetadata(
                    url = "https://trmnl.com/image.png",
                    refreshIntervalSecs = 300L,
                    timestamp = now - 350_000L,
                )
            every { imageMetadataStore.imageMetadataFlow } returns MutableStateFlow(staleMetadata)

            var refreshTriggered = false
            val firstEmission =
                ticker
                    .observeTicks(
                        intervalSeconds = 300L,
                        isLoading = { true },
                        onRefreshTriggered = { refreshTriggered = true },
                        clock = { now },
                    ).first()

            assertThat(firstEmission).isEqualTo("any moment now")
            assertThat(refreshTriggered).isFalse()
            coVerify(exactly = 0) { trmnlWorkScheduler.triggerScheduledRefresh() }
        }

    @Test
    fun `observeTicks counts down and triggers refresh when interval elapses`() =
        runTest {
            var currentTime = 1_000_000L
            // 2 seconds remaining on a 60s interval
            val metadataFlow =
                MutableStateFlow<ImageMetadata?>(
                    ImageMetadata(
                        url = "https://trmnl.com/image.png",
                        refreshIntervalSecs = 60L,
                        timestamp = currentTime - 58_000L,
                    ),
                )
            every { imageMetadataStore.imageMetadataFlow } returns metadataFlow

            var isLoading = false
            val emissions = mutableListOf<String>()

            val job =
                launch {
                    ticker
                        .observeTicks(
                            intervalSeconds = 60L,
                            isLoading = { isLoading },
                            onRefreshTriggered = { isLoading = true },
                            clock = { currentTime },
                        ).take(3)
                        .collect { emissions.add(it) }
                }

            // Step 1: initial tick at t=0 (2s remaining)
            advanceTimeBy(100L)
            assertThat(emissions).containsExactly("in 2 seconds")

            // Step 2: advance 1s (1s remaining)
            currentTime += 1_000L
            advanceTimeBy(1_000L)
            assertThat(emissions).containsExactly("in 2 seconds", "in 1 seconds")

            // Step 3: advance 1s (0s remaining -> triggers refresh)
            currentTime += 1_000L
            advanceTimeBy(1_000L)
            job.join()

            assertThat(emissions).containsExactly("in 2 seconds", "in 1 seconds", "any moment now").inOrder()
            assertThat(isLoading).isTrue()
            coVerify(exactly = 1) { trmnlWorkScheduler.triggerScheduledRefresh() }
        }

    @Test
    fun `observeTicks counts down using stable fallback timestamp when metadata store is initially empty`() =
        runTest {
            var currentTime = 1_000_000L
            every { imageMetadataStore.imageMetadataFlow } returns MutableStateFlow(null)

            val emissions = mutableListOf<String>()
            val job =
                launch {
                    ticker
                        .observeTicks(
                            intervalSeconds = 2L,
                            isLoading = { false },
                            onRefreshTriggered = {},
                            clock = { currentTime },
                        ).take(3)
                        .collect { emissions.add(it) }
                }

            // Step 1: t=0, fallback timestamp captured at 1_000_000L -> 2s remaining
            advanceTimeBy(100L)
            assertThat(emissions).containsExactly("in 2 seconds")

            // Step 2: t=1s -> 1s remaining (proving fallback timestamp was NOT reset to new `now`)
            currentTime += 1_000L
            advanceTimeBy(1_000L)
            assertThat(emissions).containsExactly("in 2 seconds", "in 1 seconds")

            // Step 3: t=2s -> 0s remaining -> triggers refresh
            currentTime += 1_000L
            advanceTimeBy(1_000L)
            job.join()

            assertThat(emissions).containsExactly("in 2 seconds", "in 1 seconds", "any moment now").inOrder()
            coVerify(exactly = 1) { trmnlWorkScheduler.triggerScheduledRefresh() }
        }

    @Test
    fun `observeTicks advances reference timestamp on trigger to prevent rapid 5s retries after failure`() =
        runTest {
            var currentTime = 1_000_000L
            // Cached image is stale (350s old on 300s interval), and subsequent fetch fails (metadata stays unchanged)
            val staleMetadata =
                ImageMetadata(
                    url = "https://trmnl.com/image.png",
                    refreshIntervalSecs = 300L,
                    timestamp = currentTime - 350_000L,
                )
            every { imageMetadataStore.imageMetadataFlow } returns MutableStateFlow(staleMetadata)

            val emissions = mutableListOf<String>()
            val job =
                launch {
                    ticker
                        .observeTicks(
                            intervalSeconds = 300L,
                            isLoading = { false },
                            onRefreshTriggered = {},
                            clock = { currentTime },
                        ).take(2)
                        .collect { emissions.add(it) }
                }

            // First tick: stale -> triggers refresh and sets reference timestamp to `currentTime`
            advanceTimeBy(100L)
            assertThat(emissions).containsExactly("any moment now")
            coVerify(exactly = 1) { trmnlWorkScheduler.triggerScheduledRefresh() }

            // 5 seconds later: fetch failed so metadata store did NOT update, and `isLoading` is false.
            // Because reference timestamp was advanced to `currentTime`, remaining time is 295s ("in 4 minutes")
            // instead of re-triggering another refresh 5 seconds later!
            currentTime += 5_000L
            advanceTimeBy(5_000L)
            job.join()

            assertThat(emissions).containsExactly("any moment now", "in 4 minutes").inOrder()
            coVerify(exactly = 1) { trmnlWorkScheduler.triggerScheduledRefresh() }
        }

    @Test
    fun `observeTicks resets countdown when background WorkManager updates ImageMetadataStore`() =
        runTest {
            var currentTime = 1_000_000L
            // 10 seconds remaining on a 300s (5m) interval
            val metadataFlow =
                MutableStateFlow<ImageMetadata?>(
                    ImageMetadata(
                        url = "https://trmnl.com/image1.png",
                        refreshIntervalSecs = 300L,
                        timestamp = currentTime - 290_000L,
                    ),
                )
            every { imageMetadataStore.imageMetadataFlow } returns metadataFlow

            val emissions = mutableListOf<String>()
            val job =
                launch {
                    ticker
                        .observeTicks(
                            intervalSeconds = 300L,
                            isLoading = { false },
                            onRefreshTriggered = {},
                            clock = { currentTime },
                        ).take(2)
                        .collect { emissions.add(it) }
                }

            advanceTimeBy(100L)
            assertThat(emissions).containsExactly("in 10 seconds")

            // Background periodic WorkManager finishes and updates metadata store with fresh timestamp
            currentTime += 1_000L
            metadataFlow.value =
                ImageMetadata(
                    url = "https://trmnl.com/image2.png",
                    refreshIntervalSecs = 300L,
                    timestamp = currentTime,
                )

            advanceTimeBy(1_000L)
            job.join()

            assertThat(emissions).containsExactly("in 10 seconds", "in 5 minutes").inOrder()
            coVerify(exactly = 0) { trmnlWorkScheduler.triggerScheduledRefresh() }
        }
}
