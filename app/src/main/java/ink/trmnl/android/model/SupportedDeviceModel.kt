package ink.trmnl.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Simplified device configuration DTO for UI purposes.
 *
 * This is a lightweight, Parcelable version of [ink.trmnl.android.network.model.TrmnlDeviceModel]
 * containing only the essential information needed for device selection and display.
 *
 * @property name Unique identifier for the model (e.g., "v2", "kindle", "byod")
 * @property label Human-readable label for the model (e.g., "TRMNL X")
 * @property description Description of the model
 * @property width Display width in pixels
 * @property height Display height in pixels
 * @property colors Number of colors supported
 * @property bitDepth Bit depth of the display
 * @property scaleFactor Scale factor for rendering
 * @property rotation Display rotation in degrees
 * @property mimeType MIME type of the image format (e.g., "image/png")
 * @property kind Device kind (e.g., "trmnl", "kindle", "byod", "tidbyt")
 */
@Parcelize
data class SupportedDeviceModel(
    val name: String,
    val label: String,
    val description: String,
    val width: Int,
    val height: Int,
    val colors: Int,
    val bitDepth: Int,
    val scaleFactor: Double,
    val rotation: Int,
    val mimeType: String,
    val kind: String,
) : Parcelable
