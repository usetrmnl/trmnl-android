package ink.trmnl.android.work

import com.squareup.anvil.annotations.optional.SingleIn
import ink.trmnl.android.data.ImageMetadata
import ink.trmnl.android.data.ImageMetadataStore
import ink.trmnl.android.di.AppScope
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Manages the image update process for the TRMNL display.
 *
 * This manager handles communication between the background image refresh workers
 * and the UI components that display the TRMNL images. It maintains a flow of
 * image metadata that UI components can observe to update their content.
 */
@SingleIn(AppScope::class)
class TrmnlImageUpdateManager
    @Inject
    constructor(
        private val imageMetadataStore: ImageMetadataStore,
    ) {
        private val _imageUpdateFlow = MutableStateFlow<ImageMetadata?>(null)
        val imageUpdateFlow: StateFlow<ImageMetadata?> = _imageUpdateFlow.asStateFlow()

        /**
         * Updates the image URL and notifies observers through the flow.
         * Only accepts updates with newer timestamps than the current image.
         */
        fun updateImage(
            imageUrl: String,
            refreshIntervalSecs: Long? = null,
            errorMessage: String? = null,
        ) {
            val imageMetadata =
                ImageMetadata(
                    url = imageUrl,
                    refreshIntervalSecs = refreshIntervalSecs,
                    errorMessage = errorMessage,
                )

            Timber.d("Updating image URL from TrmnlImageUpdateManager: $imageMetadata")

            _imageUpdateFlow.value = imageMetadata
        }

        /**
         * Initialize the manager with the last cached image URL if available
         */
        suspend fun initialize() {
            imageMetadataStore.imageMetadataFlow.collect { metadata ->
                if (metadata != null && _imageUpdateFlow.value == null) {
                    Timber.d("Initializing image URL from ImageMetadataStore cache: ${metadata.url}")
                    _imageUpdateFlow.value = metadata
                }
            }
        }
    }
