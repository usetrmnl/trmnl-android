package ink.trmnl.android.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.work.WorkInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Represents the display information for the next image refresh.
 * @see [WorkInfo.nextRunTime]
 */
data class NextImageRefreshDisplayInfo(
    val workerState: WorkInfo.State,
    val timeUntilNextRefresh: String,
    val nextRefreshOnDateTime: String,
    val nextRefreshTimeMillis: Long,
)

fun WorkInfo.State?.toDisplayString(): String =
    when (this) {
        WorkInfo.State.ENQUEUED -> "Scheduled"
        WorkInfo.State.RUNNING -> "Running now"
        WorkInfo.State.SUCCEEDED -> "Completed successfully"
        WorkInfo.State.FAILED -> "Failed"
        WorkInfo.State.BLOCKED -> "Waiting for conditions"
        WorkInfo.State.CANCELLED -> "Cancelled\nValidate and continue to reschedule"
        null -> "Unknown"
    }

@Composable
fun WorkInfo.State?.toColor(): Color =
    when (this) {
        WorkInfo.State.ENQUEUED -> MaterialTheme.colorScheme.primary
        WorkInfo.State.RUNNING -> MaterialTheme.colorScheme.tertiary
        WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.primary
        WorkInfo.State.FAILED -> MaterialTheme.colorScheme.error
        WorkInfo.State.BLOCKED -> MaterialTheme.colorScheme.secondary
        WorkInfo.State.CANCELLED -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurface
    }

fun WorkInfo.State?.toIcon(): ImageVector =
    when (this) {
        WorkInfo.State.ENQUEUED -> Icons.Default.Refresh
        WorkInfo.State.RUNNING -> Icons.Default.PlayArrow
        WorkInfo.State.SUCCEEDED -> Icons.Default.CheckCircle
        WorkInfo.State.FAILED -> Icons.Default.Warning
        WorkInfo.State.BLOCKED -> Icons.Default.Clear
        WorkInfo.State.CANCELLED -> Icons.Default.Clear
        null -> Icons.Default.Refresh
    }

fun WorkInfo?.nextRunTime(): NextImageRefreshDisplayInfo? {
    if (this == null) {
        return null
    }

    val formatter = DateTimeFormatter.ofPattern("MMM dd 'at' hh:mm:ss a")
    val nextRunTime: String =
        Instant
            .ofEpochMilli(nextScheduleTimeMillis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)

    val timeUntil = nextScheduleTimeMillis - Instant.now().toEpochMilli()
    val timeUntilText =
        when {
            timeUntil <= 0 -> "any moment now"
            timeUntil < 60000 -> "in ${timeUntil / 1000} seconds"
            timeUntil < 3600000 -> "in ${timeUntil / 60000} minutes"
            else -> "in ${timeUntil / 3600000} hours"
        }
    return NextImageRefreshDisplayInfo(
        workerState = state,
        timeUntilNextRefresh = timeUntilText,
        nextRefreshOnDateTime = nextRunTime,
        nextRefreshTimeMillis = nextScheduleTimeMillis,
    )
}
