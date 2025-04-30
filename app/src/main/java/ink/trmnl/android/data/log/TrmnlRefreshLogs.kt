package ink.trmnl.android.data.log

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TrmnlRefreshLogs(
    val logs: List<TrmnlRefreshLog> = emptyList(),
)
