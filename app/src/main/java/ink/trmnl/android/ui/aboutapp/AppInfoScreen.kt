package ink.trmnl.android.ui.aboutapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import ink.trmnl.android.BuildConfig
import ink.trmnl.android.R
import ink.trmnl.android.data.AppConfig.TRMNL_ANDROID_APP_GITHUB_URL
import ink.trmnl.android.data.AppConfig.TRMNL_ANDROID_DOCUMENTATION_URL
import ink.trmnl.android.data.AppConfig.TRMNL_SITE_URL
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.ui.aboutapp.AppInfoScreen.Event
import ink.trmnl.android.ui.aboutapp.AppInfoScreen.State
import ink.trmnl.android.ui.icons.Icons
import ink.trmnl.android.ui.theme.TrmnlDisplayAppTheme
import ink.trmnl.android.ui.theme.TrmnlOrange
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying app information and links.
 */
@Parcelize
data object AppInfoScreen : Screen {
    data class State(
        val appVersion: String,
        val buildType: String,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event {
        data object BackPressed : Event()

        data object OpenGithub : Event()

        data object OpenTrmnlSite : Event()

        data object OpenTrmnlAndroidDoc : Event()
    }
}

@AssistedInject
class AppInfoPresenter(
    @Assisted private val navigator: Navigator,
) : Presenter<AppInfoScreen.State> {
    @Composable
    override fun present(): AppInfoScreen.State {
        val uriHandler = LocalUriHandler.current
        val appVersion = BuildConfig.VERSION_NAME
        val buildType = BuildConfig.BUILD_TYPE

        return State(
            appVersion = appVersion,
            buildType = buildType,
            eventSink = { event ->
                when (event) {
                    Event.BackPressed -> {
                        navigator.pop()
                    }

                    Event.OpenGithub -> {
                        uriHandler.openUri(TRMNL_ANDROID_APP_GITHUB_URL)
                    }

                    Event.OpenTrmnlSite -> {
                        uriHandler.openUri(TRMNL_SITE_URL)
                    }

                    Event.OpenTrmnlAndroidDoc -> {
                        uriHandler.openUri(TRMNL_ANDROID_DOCUMENTATION_URL)
                    }
                }
            },
        )
    }

    @CircuitInject(AppInfoScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): AppInfoPresenter
    }
}

@CircuitInject(AppInfoScreen::class, AppScope::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoContent(
    state: State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("About App") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(Event.BackPressed) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Hero Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.trmnl_logo_plain),
                    contentDescription = null,
                    tint = TrmnlOrange,
                    modifier = Modifier.size(72.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Display TRMNL e-ink content on your Android devices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Application Details Card
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Application Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    AppInfoRow(
                        label = "Version",
                        value = state.appVersion,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    AppInfoRow(
                        label = "Build Type",
                        value = state.buildType.replaceFirstChar { it.uppercase() },
                    )
                }
            }

            // Connect & Learn Resources Card
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "Connect & Learn",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ResourceLinkItem(
                        title = "GitHub Repository",
                        subtitle = "Source code, contributions & issue tracking",
                        iconPainter = painterResource(R.drawable.github_filled),
                        onClick = { state.eventSink(Event.OpenGithub) },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    ResourceLinkItem(
                        title = "TRMNL Android Apps",
                        subtitle = "Learn about other Android apps for TRMNL",
                        iconVector = Icons.Default.Android,
                        onClick = { state.eventSink(Event.OpenTrmnlAndroidDoc) },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    ResourceLinkItem(
                        title = "Official TRMNL Website",
                        subtitle = "Learn more about TRMNL e-ink hardware",
                        iconPainter = painterResource(R.drawable.outline_link_2_24),
                        onClick = { state.eventSink(Event.OpenTrmnlSite) },
                    )
                }
            }

            // Footer Attribution
            Text(
                text = "TRMNL Android is open source under the MIT License",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun AppInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ResourceLinkItem(
    title: String,
    subtitle: String,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                } else if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun AppInfoScreenPreview() {
    TrmnlDisplayAppTheme {
        AppInfoContent(
            state =
                State(
                    appVersion = "1.0.0",
                    buildType = "debug",
                    eventSink = {},
                ),
        )
    }
}
