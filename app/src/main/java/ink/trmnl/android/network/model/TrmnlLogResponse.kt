package ink.trmnl.android.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TrmnlLogResponse(
    @Json(name = "status") val status: Int,
    @Json(name = "error") val error: String? = null,
    // Add other fields if available in the actual response
)
