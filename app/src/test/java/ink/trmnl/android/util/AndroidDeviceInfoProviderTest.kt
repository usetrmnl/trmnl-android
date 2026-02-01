package ink.trmnl.android.util

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AndroidDeviceInfoProvider].
 */
class AndroidDeviceInfoProviderTest {
    private lateinit var context: Context
    private lateinit var provider: AndroidDeviceInfoProvider
    private lateinit var batteryManager: BatteryManager
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiInfo: WifiInfo

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        batteryManager = mockk()
        wifiManager = mockk()
        wifiInfo = mockk()

        provider = AndroidDeviceInfoProvider(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Battery Level Tests

    @Test
    fun `getBatteryLevel returns valid percentage when battery manager available`() {
        // Arrange
        val expectedBatteryLevel = 85
        every { context.getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
        every { batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns expectedBatteryLevel

        // Act
        val result = provider.getBatteryLevel()

        // Assert
        assertThat(result).isEqualTo(expectedBatteryLevel)
    }

    @Test
    fun `getBatteryLevel returns null when battery manager unavailable`() {
        // Arrange
        every { context.getSystemService(Context.BATTERY_SERVICE) } returns null

        // Act
        val result = provider.getBatteryLevel()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getBatteryLevel returns null when exception occurs`() {
        // Arrange
        every { context.getSystemService(Context.BATTERY_SERVICE) } throws RuntimeException("Test exception")

        // Act
        val result = provider.getBatteryLevel()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getBatteryLevel returns 0 when battery level is 0`() {
        // Arrange
        every { context.getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
        every { batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 0

        // Act
        val result = provider.getBatteryLevel()

        // Assert
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `getBatteryLevel returns 100 when battery is full`() {
        // Arrange
        every { context.getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
        every { batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 100

        // Act
        val result = provider.getBatteryLevel()

        // Assert
        assertThat(result).isEqualTo(100)
    }

    // WiFi Signal Strength Tests

    @Test
    fun `getWifiSignalStrength returns valid RSSI when WiFi connected`() {
        // Arrange
        val expectedRssi = -65 // Good signal strength
        every { context.applicationContext.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.connectionInfo } returns wifiInfo
        every { wifiInfo.rssi } returns expectedRssi

        // Act
        val result = provider.getWifiSignalStrength()

        // Assert
        assertThat(result).isEqualTo(expectedRssi)
    }

    @Test
    fun `getWifiSignalStrength returns null when WiFi manager unavailable`() {
        // Arrange
        every { context.applicationContext.getSystemService(Context.WIFI_SERVICE) } returns null

        // Act
        val result = provider.getWifiSignalStrength()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getWifiSignalStrength returns null when WiFi info unavailable`() {
        // Arrange
        every { context.applicationContext.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.connectionInfo } returns null

        // Act
        val result = provider.getWifiSignalStrength()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getWifiSignalStrength returns null when RSSI is -127 (no signal)`() {
        // Arrange
        every { context.applicationContext.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.connectionInfo } returns wifiInfo
        every { wifiInfo.rssi } returns -127 // Special value indicating no signal

        // Act
        val result = provider.getWifiSignalStrength()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getWifiSignalStrength handles getIntProperty exception`() {
        // Arrange
        every { context.applicationContext.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.connectionInfo } returns wifiInfo
        every { wifiInfo.rssi } throws RuntimeException("Access denied")

        // Act
        val result = provider.getWifiSignalStrength()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getWifiSignalStrength returns null when exception occurs`() {
        // Arrange
        every { context.applicationContext.getSystemService(Context.WIFI_SERVICE) } throws RuntimeException("Test exception")

        // Act
        val result = provider.getWifiSignalStrength()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getWifiSignalStrength returns strong signal value`() {
        // Arrange
        val strongSignal = -30 // Excellent signal
        every { context.applicationContext.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.connectionInfo } returns wifiInfo
        every { wifiInfo.rssi } returns strongSignal

        // Act
        val result = provider.getWifiSignalStrength()

        // Assert
        assertThat(result).isEqualTo(strongSignal)
    }

    @Test
    fun `getWifiSignalStrength returns weak signal value`() {
        // Arrange
        val weakSignal = -90 // Very weak signal
        every { context.applicationContext.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.connectionInfo } returns wifiInfo
        every { wifiInfo.rssi } returns weakSignal

        // Act
        val result = provider.getWifiSignalStrength()

        // Assert
        assertThat(result).isEqualTo(weakSignal)
    }

    @Test
    fun `getWifiSignalStrength returns medium signal value`() {
        // Arrange
        val mediumSignal = -70 // Good signal
        every { context.applicationContext.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.connectionInfo } returns wifiInfo
        every { wifiInfo.rssi } returns mediumSignal

        // Act
        val result = provider.getWifiSignalStrength()

        // Assert
        assertThat(result).isEqualTo(mediumSignal)
    }
}
