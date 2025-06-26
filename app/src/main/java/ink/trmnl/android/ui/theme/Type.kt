package ink.trmnl.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import ink.trmnl.android.R

// Create FontFamily from bundled font resources
// These are included directly in the app package for F-Droid compatibility
val ebGaramondFontFamily =
    FontFamily(
        Font(R.font.eb_garamond_regular, FontWeight.Normal),
        Font(R.font.eb_garamond_medium, FontWeight.Medium),
        Font(R.font.eb_garamond_semibold, FontWeight.SemiBold),
        Font(R.font.eb_garamond_bold, FontWeight.Bold),
        Font(R.font.eb_garamond_extrabold, FontWeight.ExtraBold),
        Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.eb_garamond_medium_italic, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.eb_garamond_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
        Font(R.font.eb_garamond_bold_italic, FontWeight.Bold, FontStyle.Italic),
        Font(R.font.eb_garamond_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    )

// Both body and display use the same font family
val bodyFontFamily = ebGaramondFontFamily
val displayFontFamily = ebGaramondFontFamily

// Default Material 3 typography values
val baseline = Typography()

val AppTypography =
    Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
    )
