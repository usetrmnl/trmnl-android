package ink.trmnl.android.model

import androidx.annotation.Keep

/**
 * Represents a user's device model selection preference.
 *
 * @property name Unique identifier for the model (e.g., "amazon_kindle_2024")
 * @property label Human-readable label for display (e.g., "Amazon Kindle 2024")
 */
@Keep
data class DeviceModelSelection(
    val name: String,
    val label: String,
)
