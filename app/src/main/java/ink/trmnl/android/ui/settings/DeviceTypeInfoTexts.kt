package ink.trmnl.android.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
import ink.trmnl.android.ui.theme.TrmnlDisplayAppTheme

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
                    url = "https://usetrmnl.com/dashboard",
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { uriHandler.openUri("https://usetrmnl.com/dashboard") },
                ),
            ) {
                withStyle(style = linkStyle) {
                    append("dashboard")
                }
            }

            append(". ")

            withLink(
                LinkAnnotation.Url(
                    url = "https://docs.usetrmnl.com/go/private-api/introduction",
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { uriHandler.openUri("https://docs.usetrmnl.com/go/private-api/introduction") },
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

@Preview(name = "Info Text View Preview", showBackground = true)
@Composable
private fun PreviewInfoTextView() {
    TrmnlDisplayAppTheme {
        TrmnlDeviceTypeInfoText()
    }
}