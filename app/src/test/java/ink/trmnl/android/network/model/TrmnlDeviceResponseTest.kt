package ink.trmnl.android.network.model

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test

/**
 * Unit tests for [TrmnlDeviceResponse] and [TrmnlDevice].
 *
 * These tests verify JSON parsing of the /api/devices endpoint responses.
 */
class TrmnlDeviceResponseTest {
    private val moshi =
        Moshi
            .Builder()
            .build()

    private val deviceResponseAdapter = moshi.adapter(TrmnlDeviceResponse::class.java)

    @Test
    fun `parse device response from JSON successfully`() {
        // Arrange - Load the test JSON from resources
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Assert that we successfully loaded the JSON
        assertThat(json).isNotNull()

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)

        // Assert
        assertThat(response).isNotNull()
        assertThat(response?.data).isNotNull()
    }

    @Test
    fun `parse device with all fields correctly`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)
        val device = response?.data

        // Assert - Verify all fields
        assertThat(device).isNotNull()
        assertThat(device?.id).isEqualTo(123)
        assertThat(device?.name).isEqualTo("My TRMNL")
        assertThat(device?.friendlyId).isEqualTo("ABC-123")
        assertThat(device?.macAddress).isEqualTo("12:34:56:78:9A:BC")
        assertThat(device?.batteryVoltage).isEqualTo(3.7)
        assertThat(device?.rssi).isEqualTo(-70)
        assertThat(device?.sleepModeEnabled).isEqualTo(false)
        assertThat(device?.sleepStartTime).isEqualTo(1320)
        assertThat(device?.sleepEndTime).isEqualTo(480)
        assertThat(device?.percentCharged).isEqualTo(85.0)
        assertThat(device?.wifiStrength).isEqualTo(75.0)
    }

    @Test
    fun `parse device with nullable fields as null`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response_nullable_fields.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)
        val device = response?.data

        // Assert - Verify nullable fields are null
        assertThat(device).isNotNull()
        assertThat(device?.id).isEqualTo(456)
        assertThat(device?.name).isEqualTo("Test Device")
        assertThat(device?.batteryVoltage).isNull()
        assertThat(device?.rssi).isNull()
        assertThat(device?.sleepModeEnabled).isNull()
        assertThat(device?.sleepStartTime).isNull()
        assertThat(device?.sleepEndTime).isNull()
        assertThat(device?.percentCharged).isEqualTo(100.0)
        assertThat(device?.wifiStrength).isEqualTo(100.0)
    }

    @Test
    fun `parse device with edge case values`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response_edge_cases.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)
        val device = response?.data

        // Assert - Verify edge case values
        assertThat(device).isNotNull()
        assertThat(device?.id).isEqualTo(789)
        assertThat(device?.sleepModeEnabled).isEqualTo(true)
        assertThat(device?.sleepStartTime).isEqualTo(0) // Midnight
        assertThat(device?.sleepEndTime).isEqualTo(1439) // 11:59 PM
        assertThat(device?.percentCharged).isEqualTo(0.0) // Minimum
        assertThat(device?.wifiStrength).isEqualTo(0.0) // Minimum
    }

    @Test
    fun `parse sleep time values correctly`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)
        val device = response?.data

        // Assert - Verify time calculations
        assertThat(device).isNotNull()
        // 1320 minutes from midnight = 22:00 (10:00 PM)
        assertThat(device?.sleepStartTime).isEqualTo(1320)
        // 480 minutes from midnight = 08:00 (8:00 AM)
        assertThat(device?.sleepEndTime).isEqualTo(480)
    }

    @Test
    fun `parse battery and wifi percentages within valid range`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)
        val device = response?.data

        // Assert - Verify percentages are in valid range (0-100)
        assertThat(device).isNotNull()
        assertThat(device?.percentCharged).isAtLeast(0.0)
        assertThat(device?.percentCharged).isAtMost(100.0)
        assertThat(device?.wifiStrength).isAtLeast(0.0)
        assertThat(device?.wifiStrength).isAtMost(100.0)
    }

    @Test
    fun `parse MAC address format correctly`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)
        val device = response?.data

        // Assert - Verify MAC address format
        assertThat(device).isNotNull()
        assertThat(device?.macAddress).matches("[0-9A-Fa-f:]{17}")
        assertThat(device?.macAddress).contains(":")
    }

    @Test
    fun `parse battery voltage as double`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)
        val device = response?.data

        // Assert - Verify battery voltage is a Double type
        assertThat(device).isNotNull()
        assertThat(device?.batteryVoltage).isInstanceOf(Double::class.java)
        assertThat(device?.batteryVoltage).isGreaterThan(0.0)
    }

    @Test
    fun `parse RSSI as negative integer`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)
        val device = response?.data

        // Assert - Verify RSSI is typically negative (dBm)
        assertThat(device).isNotNull()
        assertThat(device?.rssi).isInstanceOf(Int::class.java)
        assertThat(device?.rssi).isLessThan(0)
    }

    @Test
    fun `verify all JSON field mappings`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("device_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = deviceResponseAdapter.fromJson(json!!)
        val device = response?.data

        // Assert - Verify snake_case to camelCase mapping
        assertThat(device).isNotNull()
        // Verify that JSON snake_case fields are mapped correctly
        assertThat(device?.friendlyId).isNotNull() // friendly_id -> friendlyId
        assertThat(device?.macAddress).isNotNull() // mac_address -> macAddress
        assertThat(device?.batteryVoltage).isNotNull() // battery_voltage -> batteryVoltage
        assertThat(device?.sleepModeEnabled).isNotNull() // sleep_mode_enabled -> sleepModeEnabled
        assertThat(device?.sleepStartTime).isNotNull() // sleep_start_time -> sleepStartTime
        assertThat(device?.sleepEndTime).isNotNull() // sleep_end_time -> sleepEndTime
        assertThat(device?.percentCharged).isNotNull() // percent_charged -> percentCharged
        assertThat(device?.wifiStrength).isNotNull() // wifi_strength -> wifiStrength
    }
}
