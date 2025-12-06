package ink.trmnl.android.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import ink.trmnl.android.R

/**
 * Local Material Symbols icons.
 * Replaces deprecated androidx.compose.material.icons package.
 *
 * Icons downloaded from https://fonts.google.com/icons (Material Symbols)
 * and converted to Android Vector Drawables.
 */
object Icons {
    object Default {
        val CheckCircle: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_check_circle_24dp)

        val Clear: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_clear_24dp)

        val DateRange: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_date_range_24dp)

        val PlayArrow: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_play_arrow_24dp)

        val Refresh: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_refresh_24dp)

        val Settings: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_settings_24dp)

        val Share: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_share_24dp)

        val Warning: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_warning_24dp)
    }

    object Outlined {
        val Info: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_info_outlined_24dp)

        val Warning: ImageVector
            @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_warning_outlined_24dp)
    }

    object AutoMirrored {
        object Filled {
            val ArrowBack: ImageVector
                @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_arrow_back_24dp)

            val ArrowForward: ImageVector
                @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_arrow_forward_24dp)

            val List: ImageVector
                @Composable get() = ImageVector.vectorResource(id = R.drawable.ic_list_24dp)
        }
    }
}
