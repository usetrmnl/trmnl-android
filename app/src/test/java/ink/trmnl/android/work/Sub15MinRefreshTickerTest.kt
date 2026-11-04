package ink.trmnl.android.work

import com.google.common.truth.Truth.assertThat
import ink.trmnl.android.data.ImageMetadata
import ink.trmnl.android.data.ImageMetadataStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
        every { trmnlWorkScheduler.isImageRefreshWorkRunningSync() } returns false
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
            verify(exactly = 0) { trmnlWorkScheduler.triggerScheduledRefresh() }
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
            verify(exactly = 1) { trmnlWorkScheduler.triggerScheduledRefresh() }
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
            every { trmnlWorkScheduler.isImageRefreshWorkRunningSync() } returns true

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
            verify(exactly = 0) { trmnlWorkScheduler.triggerScheduledRefresh() }
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
            verify(exactly = 0) { trmnlWorkScheduler.triggerScheduledRefresh() }
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
            verify(exactly = 1) { trmnlWorkScheduler.triggerScheduledRefresh() }
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
            verify(exactly = 0) { trmnlWorkScheduler.triggerScheduledRefresh() }
        }
}
