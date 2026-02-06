package ink.trmnl.android.ui.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.ui.icons.Icons
import ink.trmnl.android.ui.theme.TrmnlDisplayAppTheme

/**
 * Displays device type-specific information text based on the selected device type.
 *
 * This composable function uses a [Crossfade] animation to smoothly transition between different
 * information text views when the user changes the selected device type. Each device type
 * (TRMNL, BYOD, BYOS) has its own dedicated information text view that provides relevant
 * context and documentation links.
 *
 * @param deviceType The currently selected [TrmnlDeviceType] that determines which information
 *                  text view to display.
 */
@Composable
internal fun SwitchDeviceTypeInfoText(deviceType: TrmnlDeviceType) {
    Crossfade(targetState = deviceType, label = "DeviceTypeCrossfade") { type ->
        when (type) {
            TrmnlDeviceType.TRMNL -> TrmnlDeviceTypeInfoText()
            TrmnlDeviceType.BYOD -> ByodDeviceTypeInfoText()
            TrmnlDeviceType.BYOS -> ByosDeviceTypeInfoText()
        }
    }
}

@Composable
internal fun TrmnlDeviceTypeInfoText() {
    // Informational text with links using withLink
    val uriHandler = LocalUriHandler.current
    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    val annotatedString =
        buildAnnotatedString {
            append("Your TRMNL device token can be found in settings screen from your ")

            withLink(
                LinkAnnotation.Url(
                    url = "https://trmnl.com/dashboard",
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { uriHandler.openUri("https://trmnl.com/dashboard") },
                ),
            ) {
                withStyle(style = linkStyle) {
                    append("dashboard")
                }
            }

            append(". ")

            withLink(
                LinkAnnotation.Url(
                    url = "https://docs.trmnl.com/go/private-api/introduction",
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { uriHandler.openUri("https://docs.trmnl.com/go/private-api/introduction") },
                ),
            ) {
                withStyle(style = linkStyle) {
                    append("Learn more")
                }
            }

            append(".")
        }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Information",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
internal fun ByodDeviceTypeInfoText() {
    // Informational text with link using withLink for BYOD
    val uriHandler = LocalUriHandler.current
    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    val annotatedString =
        buildAnnotatedString {
            append("Bring your own device (BYOD) works like a master TRMNL device. You will need BYOD add-on. ")

            withLink(
                LinkAnnotation.Url(
                    url = "https://docs.trmnl.com/go/diy/byod-s",
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { uriHandler.openUri("https://docs.trmnl.com/go/diy/byod-s") },
                ),
            ) {
                withStyle(style = linkStyle) {
                    append("Learn more")
                }
            }

            append(".")
        }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Information",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
internal fun ByosDeviceTypeInfoText() {
    // Informational text with link using withLink for BYOS
    val uriHandler = LocalUriHandler.current
    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    val annotatedString =
        buildAnnotatedString {
            append("Bring your own server (BYOS) config.")

            withLink(
                LinkAnnotation.Url(
                    url = "https://docs.trmnl.com/go/diy/byos",
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { uriHandler.openUri("https://docs.trmnl.com/go/diy/byos") },
                ),
            ) {
                withStyle(style = linkStyle) {
                    append("Learn more")
                }
            }

            append(".")
        }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Information",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Preview(name = "TRMNL Info Text View Preview", showBackground = true)
@Composable
private fun PreviewInfoTextView() {
    TrmnlDisplayAppTheme {
        TrmnlDeviceTypeInfoText()
    }
}

@Preview(name = "BYOD Info Text View Preview", showBackground = true)
@Composable
private fun PreviewByodInfoTextView() {
    TrmnlDisplayAppTheme {
        ByodDeviceTypeInfoText()
    }
}

@Preview(name = "BYOS Info Text View Preview", showBackground = true)
@Composable
private fun PreviewByosInfoTextView() {
    TrmnlDisplayAppTheme {
        ByosDeviceTypeInfoText()
    }
}
