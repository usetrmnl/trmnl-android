package ink.trmnl.android.util

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for the getTimeElapsedString function in DateTimeFormatter.kt.
 *
 * Note: These tests use relative timestamps based on System.currentTimeMillis() to avoid
 * static mocking issues. Each test creates a timestamp that is a fixed duration in the past.
 */
class DateTimeFormatterTest {
    @Test
    fun `getTimeElapsedString returns 'Just now' for timestamps less than a minute ago`() {
        // Timestamp from 30 seconds ago
        val timestamp = System.currentTimeMillis() - 30_000L

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("Just now")
    }

    @Test
    fun `getTimeElapsedString returns correct singular minute format`() {
        // Timestamp from 1 minute ago
        val timestamp = System.currentTimeMillis() - 60_000L

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("1 minute ago")
    }

    @Test
    fun `getTimeElapsedString returns correct plural minutes format`() {
        // Timestamp from 9 minutes ago
        val timestamp = System.currentTimeMillis() - (9 * 60_000L)

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("9 minutes ago")
    }

    @Test
    fun `getTimeElapsedString returns correct singular hour format`() {
        // Timestamp from 1 hour ago exactly
        val timestamp = System.currentTimeMillis() - (60 * 60_000L)

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("1 hour ago")
    }

    @Test
    fun `getTimeElapsedString returns correct format for hours and minutes`() {
        // Timestamp from 3 hours and 23 minutes ago
        val timestamp = System.currentTimeMillis() - (3 * 60 * 60_000L) - (23 * 60_000L)

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("3 hours and 23 minutes ago")
    }

    @Test
    fun `getTimeElapsedString returns correct format for singular hour and singular minute`() {
        // Timestamp from 1 hour and 1 minute ago
        val timestamp = System.currentTimeMillis() - (1 * 60 * 60_000L) - (1 * 60_000L)

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("1 hour and 1 minute ago")
    }

    @Test
    fun `getTimeElapsedString returns correct format for singular day`() {
        // Timestamp from 1 day ago exactly
        val timestamp = System.currentTimeMillis() - (24 * 60 * 60_000L)

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("1 day ago")
    }

    @Test
    fun `getTimeElapsedString returns correct format for plural days`() {
        // Timestamp from 2 days ago exactly
        val timestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60_000L)

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("2 days ago")
    }

    @Test
    fun `getTimeElapsedString returns correct format for days and hours`() {
        // Timestamp from 2 days and 5 hours ago (no minutes)
        val timestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60_000L) - (5 * 60 * 60_000L)

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("2 days and 5 hours ago")
    }

    @Test
    fun `getTimeElapsedString returns correct format for day, hours, and minutes`() {
        // Timestamp from 2 days, 12 hours, and 45 minutes ago
        val timestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60_000L) - (12 * 60 * 60_000L) - (45 * 60_000L)

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("2 days 12 hours and 45 minutes ago")
    }

    @Test
    fun `getTimeElapsedString returns correct format for singular day, singular hour, and singular minute`() {
        // Timestamp from 1 day, 1 hour, and 1 minute ago
        val timestamp = System.currentTimeMillis() - (1 * 24 * 60 * 60_000L) - (1 * 60 * 60_000L) - (1 * 60_000L)

        val result = getTimeElapsedString(timestamp)

        assertThat(result).isEqualTo("1 day 1 hour and 1 minute ago")
    }

    @Test
    fun `formatRefreshInterval formats whole minutes correctly`() {
        assertThat(formatRefreshInterval(300L)).isEqualTo("5 min")
        assertThat(formatRefreshInterval(600L)).isEqualTo("10 min")
        assertThat(formatRefreshInterval(60L)).isEqualTo("1 min")
    }

    @Test
    fun `formatRefreshInterval formats seconds only correctly`() {
        assertThat(formatRefreshInterval(45L)).isEqualTo("45 s")
        assertThat(formatRefreshInterval(0L)).isEqualTo("0 s")
    }

    @Test
    fun `formatRefreshInterval formats minutes and seconds correctly`() {
        assertThat(formatRefreshInterval(90L)).isEqualTo("1 min 30 s")
        assertThat(formatRefreshInterval(125L)).isEqualTo("2 min 5 s")
    }

    @Test
    fun `formatRefreshInterval throws on negative seconds`() {
        assertThrows(IllegalArgumentException::class.java) {
            formatRefreshInterval(-1L)
        }
    }
}
