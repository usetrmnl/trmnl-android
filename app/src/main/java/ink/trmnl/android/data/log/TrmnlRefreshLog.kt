package ink.trmnl.android.data.log

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import ink.trmnl.android.data.HttpResponseMetadata
import ink.trmnl.android.model.TrmnlDeviceType
import java.time.Instant

@JsonClass(generateAdapter = true)
data class TrmnlRefreshLog(
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "deviceType") val trmnlDeviceType: TrmnlDeviceType,
    @Json(name = "imageUrl") val imageUrl: String?,
    @Json(name = "imageName") val imageName: String?,
    @Json(name = "refreshRateSeconds") val refreshIntervalSeconds: Long?,
    @Json(name = "success") val success: Boolean,
    @Json(name = "error") val error: String? = null,
    @Json(name = "refreshWorkType") val imageRefreshWorkType: String? = null,
    @Json(name = "httpResponseMetadata") val httpResponseMetadata: HttpResponseMetadata? = null,
) {
    companion object {
        fun createSuccess(
            trmnlDeviceType: TrmnlDeviceType,
            imageUrl: String,
            imageName: String,
            refreshIntervalSeconds: Long?,
            imageRefreshWorkType: String?,
            httpResponseMetadata: HttpResponseMetadata? = null,
        ): TrmnlRefreshLog =
            TrmnlRefreshLog(
                timestamp = Instant.now().toEpochMilli(),
                trmnlDeviceType = trmnlDeviceType,
                imageUrl = imageUrl,
                imageName = imageName,
                refreshIntervalSeconds = refreshIntervalSeconds,
                success = true,
                imageRefreshWorkType = imageRefreshWorkType,
                httpResponseMetadata = httpResponseMetadata,
            )

        fun createFailure(error: String): TrmnlRefreshLog =
            TrmnlRefreshLog(
                timestamp = Instant.now().toEpochMilli(),
                trmnlDeviceType = TrmnlDeviceType.TRMNL, // Not used
                imageUrl = null,
                imageName = null,
                refreshIntervalSeconds = null,
                success = false,
                error = error,
                imageRefreshWorkType = null,
                httpResponseMetadata = null,
            )
    }
}
