package ink.trmnl.android.util

import android.content.Context
import android.os.BatteryManager
import com.squareup.anvil.annotations.optional.SingleIn
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.di.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Provider class for accessing Android device information.
 *
 * This class provides utility methods to retrieve device-specific information
 * such as battery level, which can be used for reporting to the TRMNL API.
 */
@SingleIn(AppScope::class)
class AndroidDeviceInfoProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /**
         * Gets the current battery level of the Android device.
         *
         * @return Battery percentage (0-100), or null if unable to retrieve
         */
        fun getBatteryLevel(): Int? =
            try {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val batteryLevel =
                    batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                Timber.i("Current battery level: $batteryLevel%")
                batteryLevel
            } catch (e: Exception) {
                Timber.e(e, "Failed to get battery level")
                null
            }
    }
