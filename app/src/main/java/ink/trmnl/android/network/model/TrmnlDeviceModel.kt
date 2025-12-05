package ink.trmnl.android.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class representing a TRMNL device model from the API.
 *
 * Each model defines the display characteristics for different e-ink devices.
 *
 * Sample JSON:
 * ```json
 * {
 *   "name": "v2",
 *   "label": "TRMNL X",
 *   "description": "TRMNL X",
 *   "width": 1872,
 *   "height": 1404,
 *   "colors": 16,
 *   "bit_depth": 4,
 *   "scale_factor": 1.8,
 *   "rotation": 0,
 *   "mime_type": "image/png",
 *   "offset_x": 0,
 *   "offset_y": 0,
 *   "published_at": "2024-01-01T00:00:00.000Z",
 *   "kind": "trmnl",
 *   "palette_ids": ["bw", "gray-4", "gray-16"]
 * }
 * ```
 *
 * @property name Unique identifier for the model
 * @property label Human-readable label for the model
 * @property description Description of the model
 * @property width Display width in pixels
 * @property height Display height in pixels
 * @property colors Number of colors supported
 * @property bitDepth Bit depth of the display
 * @property scaleFactor Scale factor for rendering
 * @property rotation Display rotation in degrees
 * @property mimeType MIME type of the image format
 * @property offsetX Horizontal offset in pixels
 * @property offsetY Vertical offset in pixels
 * @property publishedAt Publication date timestamp
 * @property kind Device kind (e.g., "trmnl", "kindle", "byod", "tidbyt")
 * @property paletteIds List of supported palette identifiers
 */
@JsonClass(generateAdapter = true)
data class TrmnlDeviceModel(
    val name: String,
    val label: String,
    val description: String,
    val width: Int,
    val height: Int,
    val colors: Int,
    @Json(name = "bit_depth")
    val bitDepth: Int,
    @Json(name = "scale_factor")
    val scaleFactor: Double,
    val rotation: Int,
    @Json(name = "mime_type")
    val mimeType: String,
    @Json(name = "offset_x")
    val offsetX: Int,
    @Json(name = "offset_y")
    val offsetY: Int,
    @Json(name = "published_at")
    val publishedAt: String,
    val kind: String,
    @Json(name = "palette_ids")
    val paletteIds: List<String>,
)
