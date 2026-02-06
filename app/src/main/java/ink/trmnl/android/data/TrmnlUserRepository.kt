package ink.trmnl.android.data

import com.slack.eithernet.ApiResult
import com.slack.eithernet.exceptionOrNull
import com.squareup.anvil.annotations.optional.SingleIn
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.network.TrmnlUserApiService
import ink.trmnl.android.network.TrmnlUserApiService.Companion.USER_INFO_API_PATH
import ink.trmnl.android.network.model.TrmnlUser
import ink.trmnl.android.network.util.constructApiUrl
import ink.trmnl.android.util.AndroidDeviceInfoProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * Repository class for user-level TRMNL API operations.
 *
 * This repository handles user API token validation and deprecated battery reporting
 * functionality that required user-level authentication.
 *
 * **Note:** Most methods in this repository are deprecated as battery reporting has been
 * migrated to use the Percent-Charged header in device-level API calls.
 *
 * See:
 * - https://github.com/usetrmnl/trmnl-android/issues/252
 * - https://github.com/usetrmnl/trmnl-android/issues/239
 * - https://github.com/usetrmnl/trmnl-android/pull/253
 * - https://discord.com/channels/1281055965508141100/1466030731770855434/1469103763846463620
 */
@SingleIn(AppScope::class)
class TrmnlUserRepository
    @Inject
    constructor(
        private val userApiService: TrmnlUserApiService,
        private val repositoryConfigProvider: RepositoryConfigProvider,
        private val androidDeviceInfoProvider: AndroidDeviceInfoProvider,
    ) {
        /**
         * Validates a user API token by calling the /api/me endpoint.
         *
         * This method is used to verify user-level (account) API tokens before saving them.
         * On success, returns the user's information.
         *
         * @param apiBaseUrl The base URL of the TRMNL API server
         * @param userApiToken The user API token to validate (should start with "user_")
         * @return A Result containing TrmnlUser on success or an exception on failure
         */
        suspend fun validateUserApiToken(
            apiBaseUrl: String,
            userApiToken: String,
        ): Result<TrmnlUser> {
            Timber.i("Validating user API token")

            if (repositoryConfigProvider.shouldUseFakeData) {
                // Return fake user data in debug mode
                return Result.success(
                    TrmnlUser(
                        id = 42,
                        name = "Test User",
                        email = "test@example.com",
                        firstName = "Test",
                        lastName = "User",
                        locale = "en",
                        timeZone = "Eastern Time (US & Canada)",
                        timeZoneIana = "America/New_York",
                        utcOffset = -14400,
                    ),
                )
            }

            val result =
                userApiService.getUserInfo(
                    fullApiUrl = constructApiUrl(apiBaseUrl, USER_INFO_API_PATH),
                    accessToken = "Bearer $userApiToken",
                )

            return when (result) {
                is ApiResult.Failure -> {
                    val exception = result.exceptionOrNull()
                    Timber.e(exception, "User API token validation failed")
                    Result.failure(exception ?: Exception("Failed to validate user token"))
                }
                is ApiResult.Success -> {
                    Timber.i("User API token validated successfully: ${result.value.data.email}")
                    Result.success(result.value.data)
                }
            }
        }

        /**
         * Fetches the device ID from the TRMNL API using the device API token.
         *
         * **DEPRECATED:** Device ID fetching is no longer needed. Battery reporting now uses
         * the Percent-Charged header in /api/display call, which only requires device-level
         * authentication (Access-Token), not user-level authentication or device ID.
         *
         * This method will be removed in a future version.
         *
         * This method calls the /api/devices/me endpoint with device-level authentication
         * to retrieve device information including the device ID, which is needed for
         * user-level API calls to /api/devices/{id}.
         *
         * **Note:** This endpoint doesn't exist on the server yet, so this method
         * returns a mocked response until the server endpoint is implemented.
         *
         * @param config Device configuration containing the device API token
         * @return A Result containing the device ID on success or an exception on failure
         */
        @Deprecated("Device ID no longer needed for battery reporting. Use Percent-Charged header instead.")
        suspend fun getDeviceIdFromApi(config: TrmnlDeviceConfig): Result<Int> {
            Timber.w("getDeviceIdFromApi is deprecated. Device ID is no longer needed for battery reporting.")
            return Result.failure(
                UnsupportedOperationException(
                    "Device ID fetching is deprecated. Battery reporting now uses Percent-Charged header " +
                        "in /api/display, which doesn't require device ID or user API token.",
                ),
            )

            /* DISABLED - Device ID no longer needed for battery reporting
            Timber.i("Fetching device ID from API for device type: ${config.type}")

            // Always use mocked response since the endpoint doesn't exist yet
            // TODO: Remove this mock when the server endpoint is implemented
            val mockedDevice =
                TrmnlDevice(
                    id = 41448,
                    name = "BYOD TRMNL",
                    friendlyId = "_____",
                    macAddress = "********",
                    batteryVoltage = null,
                    rssi = null,
                    sleepModeEnabled = false,
                    sleepStartTime = 1320,
                    sleepEndTime = 480,
                    percentCharged = 100,
                    wifiStrength = 100.0,
                )

            Timber.i("Using mocked device ID: ${mockedDevice.id}")
            return Result.success(mockedDevice.id)

            /*
             * TODO: Uncomment this when the server endpoint is implemented:
             *
             * val result = apiService.getDeviceMe(
             *     fullApiUrl = constructApiUrl(config.apiBaseUrl, DEVICE_ME_API_PATH),
             *     accessToken = config.apiAccessToken,
             * )
             *
             * return when (result) {
             *     is ApiResult.Failure -> {
             *         val exception = result.exceptionOrNull()
             *         Timber.e(exception, "Failed to fetch device ID")
             *         Result.failure(exception ?: Exception("Failed to fetch device ID"))
             *     }
             *     is ApiResult.Success -> {
             *         val deviceId = result.value.data.id
             *         Timber.i("Device ID fetched successfully: $deviceId")
             *         Result.success(deviceId)
             *     }
             * }
             */
             */
        }

        /**
         * Reports the device's battery status to the TRMNL API for BYOD devices.
         *
         * **DEPRECATED:** Battery reporting now uses the Percent-Charged header in /api/display call.
         * This method is disabled and will be removed in a future version.
         *
         * Battery percentage is now automatically sent via the Percent-Charged header parameter
         * when calling getNextDisplayData() for BYOD devices, eliminating the need for a separate
         * API call and user-level authentication.
         *
         * @param config Device configuration containing device type, device ID, and user API token
         * @see TrmnlDisplayRepository.getNextDisplayData
         */
        @Deprecated("Battery reporting now uses Percent-Charged header. This method is no longer needed.")
        suspend fun reportDeviceBatteryStatus(config: TrmnlDeviceConfig) {
            // DEPRECATED: Battery reporting now happens via Percent-Charged header in /api/display
            Timber.d("Battery reporting via separate API call is deprecated. Battery is now sent via Percent-Charged header.")
            return

            /* DISABLED - Battery now reported via Percent-Charged header
            // Only report battery for BYOD devices with required configuration
            if (config.type != TrmnlDeviceType.BYOD) {
                Timber.d("Battery reporting skipped: not a BYOD device (type: ${config.type})")
                return
            }

            if (config.deviceId == null) {
                Timber.w("Battery reporting skipped: device ID is null")
                return
            }

            if (config.userApiToken == null) {
                Timber.w("Battery reporting skipped: user API token is null")
                return
            }

            // Get current battery level
            val batteryLevel = androidDeviceInfoProvider.getBatteryLevel()
            if (batteryLevel == null) {
                Timber.w("Battery reporting skipped: unable to get battery level")
                return
            }

            // Report battery status
            try {
                val result = reportBatteryStatus(config, batteryLevel)
                result.onFailure { throwable ->
                    Timber.e(throwable, "Failed to report battery status")
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during battery reporting")
            }
             */
        }

        /**
         * Reports the device's battery status to the TRMNL API.
         *
         * **DEPRECATED:** This method is no longer used. Battery reporting now uses the
         * Percent-Charged header in /api/display call instead of PATCH /api/devices/{id}.
         *
         * This method will be removed in a future version.
         *
         * @param config Device configuration containing the device ID and user API token
         * @param batteryPercent The current battery percentage (0-100)
         * @return A Result containing Unit on success or an exception on failure
         */
        @Deprecated("Battery reporting now uses Percent-Charged header. This method is no longer needed.")
        private suspend fun reportBatteryStatus(
            config: TrmnlDeviceConfig,
            batteryPercent: Int,
        ): Result<Unit> {
            // DEPRECATED: This method is no longer used
            Timber.d("reportBatteryStatus is deprecated and disabled")
            return Result.failure(
                UnsupportedOperationException(
                    "Battery reporting via PATCH /api/devices/{id} is deprecated. Use Percent-Charged header instead.",
                ),
            )

            /* DISABLED - Battery now reported via Percent-Charged header
            val deviceId = config.deviceId
            val userApiToken = config.userApiToken

            if (deviceId == null) {
                Timber.w("Cannot report battery status: device ID is null")
                return Result.failure(IllegalStateException("Device ID is required"))
            }

            if (userApiToken == null) {
                Timber.w("Cannot report battery status: user API token is null")
                return Result.failure(IllegalStateException("User API token is required"))
            }

            Timber.d("Reporting battery status: $batteryPercent% for device ID: $deviceId")

            if (repositoryConfigProvider.shouldUseFakeData) {
                // Skip API call in debug mode
                Timber.d("Skipping battery status report (fake API mode)")
                return Result.success(Unit)
            }

            val updateRequest = TrmnlDeviceUpdateRequest(percentCharged = batteryPercent)
            val apiUrl = constructApiUrl(config.apiBaseUrl, DEVICE_API_PATH.replace("{id}", deviceId.toString()))

            val result =
                userApiService.updateDevice(
                    fullApiUrl = apiUrl,
                    accessToken = "Bearer $userApiToken",
                    updateRequest = updateRequest,
                )

            return when (result) {
                is ApiResult.Failure -> {
                    val exception = result.exceptionOrNull()
                    Timber.e(exception, "Failed to report battery status")
                    Result.failure(exception ?: Exception("Failed to report battery status"))
                }
                is ApiResult.Success -> {
                    Timber.d("Battery status reported successfully")
                    Result.success(Unit)
                }
            }
             */
        }
    }
