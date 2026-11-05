package ink.trmnl.android.util

import androidx.work.WorkInfo
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.Instant

class WorkInfoExtensionsTest {
    @Test
    fun `nextRunTime returns null when WorkInfo is null`() {
        val workInfo: WorkInfo? = null
        assertThat(workInfo.nextRunTime()).isNull()
    }

    @Test
    fun `nextRunTime formats seconds minutes and singular units correctly`() {
        val now = 1_000_000L

        // Elapsed or < 1s
        val elapsedWork = mockWorkInfo(now - 5_000L)
        assertThat(elapsedWork.nextRunTime(now)?.timeUntilNextRefresh).isEqualTo("any moment now")
        val subSecWork = mockWorkInfo(now + 400L)
        assertThat(subSecWork.nextRunTime(now)?.timeUntilNextRefresh).isEqualTo("any moment now")

        // 1 second
        val oneSecWork = mockWorkInfo(now + 1_500L)
        assertThat(oneSecWork.nextRunTime(now)?.timeUntilNextRefresh).isEqualTo("in 1 second")

        // Multiple seconds
        val multiSecWork = mockWorkInfo(now + 45_000L)
        assertThat(multiSecWork.nextRunTime(now)?.timeUntilNextRefresh).isEqualTo("in 45 seconds")

        // 1 minute
        val oneMinWork = mockWorkInfo(now + 65_000L)
        assertThat(oneMinWork.nextRunTime(now)?.timeUntilNextRefresh).isEqualTo("in 1 minute")

        // Multiple minutes
        val multiMinWork = mockWorkInfo(now + 300_000L)
        assertThat(multiMinWork.nextRunTime(now)?.timeUntilNextRefresh).isEqualTo("in 5 minutes")

        // 1 hour
        val oneHourWork = mockWorkInfo(now + 3_660_000L)
        assertThat(oneHourWork.nextRunTime(now)?.timeUntilNextRefresh).isEqualTo("in 1 hour")

        // Multiple hours
        val multiHourWork = mockWorkInfo(now + 7_200_000L)
        assertThat(multiHourWork.nextRunTime(now)?.timeUntilNextRefresh).isEqualTo("in 2 hours")
    }

    @Test
    fun `toDisplayString maps WorkInfo State correctly`() {
        assertThat(WorkInfo.State.ENQUEUED.toDisplayString()).isEqualTo("Scheduled")
        assertThat(WorkInfo.State.RUNNING.toDisplayString()).isEqualTo("Running now")
        assertThat(WorkInfo.State.SUCCEEDED.toDisplayString()).isEqualTo("Completed successfully")
        assertThat(WorkInfo.State.FAILED.toDisplayString()).isEqualTo("Failed")
        assertThat(WorkInfo.State.BLOCKED.toDisplayString()).isEqualTo("Waiting for conditions")
        assertThat(WorkInfo.State.CANCELLED.toDisplayString()).contains("Cancelled")
        val nullState: WorkInfo.State? = null
        assertThat(nullState.toDisplayString()).isEqualTo("Unknown")
    }

    private fun mockWorkInfo(nextScheduleMillis: Long): WorkInfo {
        val work = mockk<WorkInfo>()
        every { work.state } returns WorkInfo.State.ENQUEUED
        every { work.nextScheduleTimeMillis } returns nextScheduleMillis
        return work
    }
}
