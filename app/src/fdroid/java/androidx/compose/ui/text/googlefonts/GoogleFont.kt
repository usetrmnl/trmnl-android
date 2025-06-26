package androidx.compose.ui.text.googlefonts

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * F-Droid compatible mock implementation of GoogleFont
 * This replaces the Google Fonts dependency with a version that uses system fonts
 */
class GoogleFont(
    val name: String,
) {
    class Provider(
        val providerAuthority: String,
        val providerPackage: String,
        val certificates: Int,
    )
}

/**
 * F-Droid compatible mock implementation of Font for Google Fonts
 */
@Suppress("UNUSED_PARAMETER")
fun Font(
    googleFont: GoogleFont,
    fontProvider: GoogleFont.Provider,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
) = FontFamily.Default // Return the default font family directly

// Extension function for FontFamily that overrides the constructor issue

/**
 * Extension function for FontFamily that creates a FontFamily from a GoogleFont.Font
 * This allows the existing code to work without changes for F-Droid builds.
 */
operator fun FontFamily.Companion.invoke(font: Any): FontFamily = FontFamily.Default

operator fun FontFamily.Companion.invoke(vararg font: Any): FontFamily = FontFamily.Default

// Override standard font families with system fonts for F-Droid
val FontFamily.Companion.SansSerif: FontFamily
    get() = FontFamily.Default

val FontFamily.Companion.Serif: FontFamily
    get() = FontFamily.Default
