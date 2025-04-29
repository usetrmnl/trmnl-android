package dev.hossain.trmnl.data.log

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class TrmnlRefreshLog(
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "imageUrl") val imageUrl: String?,
    @Json(name = "imageName") val imageName: String?,
    @Json(name = "refreshRateSeconds") val refreshIntervalSeconds: Long?,
    @Json(name = "success") val success: Boolean,
    @Json(name = "error") val error: String? = null,
    @Json(name = "refreshWorkType") val imageRefreshWorkType: String? = null,
) {
    companion object {
        fun createSuccess(
            imageUrl: String,
            imageName: String,
            refreshIntervalSeconds: Long?,
            imageRefreshWorkType: String?,
        ): TrmnlRefreshLog =
            TrmnlRefreshLog(
                timestamp = Instant.now().toEpochMilli(),
                imageUrl = imageUrl,
                imageName = imageName,
                refreshIntervalSeconds = refreshIntervalSeconds,
                success = true,
                imageRefreshWorkType = imageRefreshWorkType,
            )

        fun createFailure(error: String): TrmnlRefreshLog =
            TrmnlRefreshLog(
                timestamp = Instant.now().toEpochMilli(),
                imageUrl = null,
                imageName = null,
                refreshIntervalSeconds = null,
                success = false,
                error = error,
                imageRefreshWorkType = null,
            )
    }
}
