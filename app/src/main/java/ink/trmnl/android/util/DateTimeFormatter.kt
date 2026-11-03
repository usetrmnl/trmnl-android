package ink.trmnl.android.util

/**
 * Returns a human-readable string representing time elapsed since the timestamp.
 * Examples: "9 minutes ago", "3 hours and 23 minutes ago", "2 days 12 hours and 45 minutes ago"
 */
internal fun getTimeElapsedString(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp

    // Convert to different units
    val seconds = diffMillis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> {
            val remainingHours = hours % 24
            val remainingMinutes = minutes % 60

            when {
                remainingHours > 0 && remainingMinutes > 0 -> {
                    "$days ${if (days == 1L) "day" else "days"} $remainingHours ${if (remainingHours == 1L) "hour" else "hours"} and $remainingMinutes ${if (remainingMinutes == 1L) "minute" else "minutes"} ago"
                }

                remainingHours > 0 -> {
                    "$days ${if (days == 1L) "day" else "days"} and $remainingHours ${if (remainingHours == 1L) "hour" else "hours"} ago"
                }

                else -> {
                    "$days ${if (days == 1L) "day" else "days"} ago"
                }
            }
        }

        hours > 0 -> {
            val remainingMinutes = minutes % 60
            if (remainingMinutes > 0) {
                "$hours ${if (hours == 1L) "hour" else "hours"} and $remainingMinutes ${if (remainingMinutes == 1L) "minute" else "minutes"} ago"
            } else {
                "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            }
        }

        minutes > 0 -> {
            "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
        }

        else -> {
            "Just now"
        }
    }
}

/**
 * Formats a duration in seconds into a human-readable interval string.
 *
 * Examples:
 * - 300s -> "5 min"
 * - 60s -> "1 min"
 * - 90s -> "1 min 30 s"
 * - 45s -> "45 s"
 */
internal fun formatRefreshInterval(seconds: Long): String {
    require(seconds >= 0) { "Interval seconds must not be negative: $seconds" }
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return when {
        minutes > 0 && remainingSeconds > 0 -> "$minutes min $remainingSeconds s"
        minutes > 0 -> "$minutes min"
        else -> "$remainingSeconds s"
    }
}
