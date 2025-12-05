package ink.trmnl.android.network.model

import com.squareup.moshi.JsonClass

/**
 * Data class representing the response from the TRMNL API for device models.
 *
 * The API endpoint returns a list of all supported device models with their
 * display characteristics.
 *
 * Sample response:
 * ```json
 * {
 *   "data": [
 *     {
 *       "name": "v2",
 *       "label": "TRMNL X",
 *       "description": "TRMNL X",
 *       "width": 1872,
 *       "height": 1404,
 *       "colors": 16,
 *       "bit_depth": 4,
 *       "scale_factor": 1.8,
 *       "rotation": 0,
 *       "mime_type": "image/png",
 *       "offset_x": 0,
 *       "offset_y": 0,
 *       "published_at": "2024-01-01T00:00:00.000Z",
 *       "kind": "trmnl",
 *       "palette_ids": ["bw", "gray-4", "gray-16"]
 *     }
 *   ]
 * }
 * ```
 *
 * @property data List of device models
 * @see TrmnlDeviceModel
 */
@JsonClass(generateAdapter = true)
data class TrmnlModelsResponse(
    val data: List<TrmnlDeviceModel>,
)
