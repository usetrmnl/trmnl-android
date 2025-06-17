package ink.trmnl.android.ui.aboutapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import ink.trmnl.android.BuildConfig
import ink.trmnl.android.R
import ink.trmnl.android.data.AppConfig.TRMNL_ANDROID_APP_GITHUB_URL
import ink.trmnl.android.data.AppConfig.TRMNL_SITE_URL
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.ui.aboutapp.AppInfoScreen.Event
import ink.trmnl.android.ui.aboutapp.AppInfoScreen.State
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
    }
}

class AppInfoPresenter
    @AssistedInject
    constructor(
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
                        Event.BackPressed -> navigator.pop()
                        Event.OpenGithub -> {
                            uriHandler.openUri(TRMNL_ANDROID_APP_GITHUB_URL)
                        }
                        Event.OpenTrmnlSite -> {
                            uriHandler.openUri(TRMNL_SITE_URL)
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
                title = { Text("App Info") },
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
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                painter = painterResource(R.drawable.trmnl_logo_plain),
                contentDescription = null,
                tint = TrmnlOrange,
                modifier = Modifier.height(64.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Version: ${state.appVersion}",
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Build Type: ${state.buildType}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Image(
                painter = painterResource(R.drawable.trmnl_device_white_transparent),
                contentDescription = "TRMNL Device",
                modifier =
                    Modifier
                        .padding(vertical = 16.dp)
                        .clickable { state.eventSink(Event.OpenTrmnlSite) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { state.eventSink(Event.OpenGithub) },
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.code_24dp),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("View Project on GitHub")
            }
        }
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
