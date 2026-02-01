package ink.trmnl.android.util

import android.content.Context
import android.net.wifi.WifiManager
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
 * such as battery level and WiFi signal strength, which can be used for reporting to the TRMNL API.
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

        /**
         * Gets the current WiFi signal strength (RSSI) of the Android device.
         *
         * RSSI (Received Signal Strength Indicator) is measured in dBm and typically
         * ranges from -100 (weakest) to 0 (strongest).
         *
         * @return WiFi signal strength in dBm, or null if unable to retrieve or WiFi is not connected
         */
        @Suppress("DEPRECATION") // WifiInfo.rssi is still the standard way to get signal strength
        fun getWifiSignalStrength(): Int? =
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val wifiInfo = wifiManager?.connectionInfo
                val rssi = wifiInfo?.rssi

                if (rssi != null && rssi != -127) { // -127 (`INVALID_RSSI`) indicates no signal
                    Timber.i("Current WiFi signal strength (RSSI): $rssi dBm")
                    rssi
                } else {
                    Timber.d("WiFi not connected or signal unavailable")
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get WiFi signal strength")
                null
            }
    }
