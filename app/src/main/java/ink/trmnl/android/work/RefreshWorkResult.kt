package ink.trmnl.android.work

import androidx.annotation.Keep

/**
 * Enum class to represent the result of the refresh work by the [TrmnlImageRefreshWorker].
 */
@Keep
enum class RefreshWorkResult {
    SUCCESS,
    FAILURE,
}
