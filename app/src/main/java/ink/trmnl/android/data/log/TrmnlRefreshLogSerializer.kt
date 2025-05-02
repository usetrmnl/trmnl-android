package ink.trmnl.android.data.log

import androidx.datastore.core.Serializer
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer for [TrmnlRefreshLogs] using Moshi.
 */
object TrmnlRefreshLogSerializer : Serializer<TrmnlRefreshLogs> {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(TrmnlRefreshLogs::class.java)

    override val defaultValue: TrmnlRefreshLogs
        get() = TrmnlRefreshLogs(emptyList())

    override suspend fun readFrom(input: InputStream): TrmnlRefreshLogs =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = input.readBytes().decodeToString()
                adapter.fromJson(jsonString) ?: defaultValue
            } catch (e: Exception) {
                Timber.e(e, "Error reading activity logs")
                defaultValue
            }
        }

    override suspend fun writeTo(
        refreshLogs: TrmnlRefreshLogs,
        output: OutputStream,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = adapter.toJson(refreshLogs)
                output.write(jsonString.toByteArray())
            } catch (e: Exception) {
                Timber.e(e, "Error writing activity logs")
            }
        }
    }
}
