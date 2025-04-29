package dev.hossain.trmnl.work

import androidx.annotation.Keep
import dev.hossain.trmnl.data.TrmnlDisplayInfo

/**
 * Enum class to represent the type of refresh work.
 */
@Keep
enum class RefreshWorkType {
    /**
     * Onetime worker request is made by user manually at any time.
     */
    ONE_TIME,

    /**
     * Periodic worker request scheduled by user and time is provided by API server.
     * @see TrmnlDisplayInfo.refreshIntervalSeconds
     */
    PERIODIC,
}
