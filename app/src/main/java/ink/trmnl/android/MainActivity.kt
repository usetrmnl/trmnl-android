package ink.trmnl.android

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.remember
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import com.squareup.anvil.annotations.ContributesMultibinding
import ink.trmnl.android.di.ActivityKey
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.di.ApplicationContext
import ink.trmnl.android.ui.display.TrmnlMirrorDisplayScreen
import ink.trmnl.android.ui.theme.TrmnlDisplayAppTheme
import ink.trmnl.android.work.TrmnlImageRefreshWorker
import ink.trmnl.android.work.TrmnlImageUpdateManager
import ink.trmnl.android.work.TrmnlWorkScheduler.Companion.IMAGE_REFRESH_ONETIME_WORK_NAME
import ink.trmnl.android.work.TrmnlWorkScheduler.Companion.IMAGE_REFRESH_PERIODIC_WORK_NAME
import timber.log.Timber
import javax.inject.Inject

/**
 * Main activity for the TRMNL app.
 *
 * This activity serves as the entry point for the Android TRMNL application.
 * It can function as either a mirror for existing TRMNL devices or as a
 * standalone TRMNL display connected directly to BYOS servers.
 */
@ContributesMultibinding(AppScope::class, boundType = Activity::class)
@ActivityKey(MainActivity::class)
class MainActivity
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val circuit: Circuit,
        private val trmnlImageUpdateManager: TrmnlImageUpdateManager,
    ) : ComponentActivity() {
        @OptIn(ExperimentalSharedTransitionApi::class)
        override fun onCreate(savedInstanceState: Bundle?) {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)

            // Setup listener for TRMNL display image updates
            listenForWorkUpdates()

            setContent {
                TrmnlDisplayAppTheme {
                    // See https://slackhq.github.io/circuit/navigation/
                    val backStack = rememberSaveableBackStack(root = TrmnlMirrorDisplayScreen)
                    val navigator = rememberCircuitNavigator(backStack)

                    // See https://slackhq.github.io/circuit/circuit-content/
                    CircuitCompositionLocals(circuit) {
                        // See https://slackhq.github.io/circuit/shared-elements/
                        SharedElementTransitionLayout {
                            // See https://slackhq.github.io/circuit/overlays/
                            ContentWithOverlays {
                                NavigableCircuitContent(
                                    navigator = navigator,
                                    backStack = backStack,
                                    decoratorFactory =
                                        remember(navigator) {
                                            GestureNavigationDecorationFactory(onBackInvoked = navigator::pop)
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }

        /**
         * Sets up observers for WorkManager work updates.
         *
         * This function:
         * 1. Listens for periodic image refresh work results
         * 2. Listens for one-time image refresh work results
         * 3. Updates the application with new images when available
         * 4. Logs work status and errors
         *
         * 📚 See following sequence diagram for flow:
         * - https://github.com/usetrmnl/trmnl-android/blob/main/CONTRIBUTING.md#trmnl-app-image-loading-flow
         */
        private fun listenForWorkUpdates() {
            val workManager = WorkManager.getInstance(context)

            workManager
                .getWorkInfosLiveData(
                    WorkQuery.fromUniqueWorkNames(IMAGE_REFRESH_PERIODIC_WORK_NAME, IMAGE_REFRESH_ONETIME_WORK_NAME),
                ).observe(this) { workInfos ->
                    // ⚠️ DEV NOTE: On app launch, previously ran work info is broadcasted here,
                    // so it may result in inconsistent behavior where it remembers last result.
                    workInfos.forEach { workInfo ->
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                Timber.d("${workInfo.tags} work ${workInfo.state.name.lowercase()}: $workInfo")
                                val newImageUrl =
                                    workInfo.outputData.getString(
                                        TrmnlImageRefreshWorker.KEY_NEW_IMAGE_URL,
                                    )

                                if (newImageUrl != null) {
                                    Timber.i("New image URL from ${workInfo.tags}: $newImageUrl")
                                    trmnlImageUpdateManager.updateImage(newImageUrl)
                                }
                            }
                            WorkInfo.State.FAILED -> {
                                val error = workInfo.outputData.getString(TrmnlImageRefreshWorker.KEY_ERROR_MESSAGE)
                                Timber.e("${workInfo.tags} work failed: $error")
                                trmnlImageUpdateManager.updateImage(imageUrl = "", errorMessage = error)
                            }
                            else -> {
                                Timber.d("${workInfo.tags} work state updated: ${workInfo.state}")
                            }
                        }
                        // Even though pruning is not recommended to do frequently,
                        // we need this to avoid getting stale completed work info
                        // See https://github.com/hossain-khan/android-trmnl-display/pull/98#issuecomment-2825920626
                        // See https://github.com/hossain-khan/android-trmnl-display/pull/63#issuecomment-2817278344
                        workManager.pruneWork()
                    }
                }
        }
    }
