package ink.trmnl.android.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ink.trmnl.android.di.AppScope

@Inject
@SingleIn(AppScope::class)
class TrmnlWorkerFactory(
    private val imageRefreshWorkerFactory: TrmnlImageRefreshWorker.Factory,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        when (workerClassName) {
            TrmnlImageRefreshWorker::class.java.name -> {
                imageRefreshWorkerFactory.create(appContext, workerParameters)
            }

            else -> {
                null
            }
        }
}
