package dev.hossain.trmnl.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TrmnlTokenDataStoreTest {
    private lateinit var context: Context
    private lateinit var tokenStore: TrmnlTokenDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tokenStore = TrmnlTokenDataStore(context)
    }

    @After
    fun tearDown() =
        runTest {
            // Clean up the data store after each test
            tokenStore.clearAll()
        }

    @Test
    fun `shouldUpdateRefreshRate - given current refresh time exists and new is different and higher - returns true`() =
        runTest {
            // Arrange - Set up current refresh rate
            tokenStore.saveRefreshRateSeconds(300L)

            // Act
            val shouldUpdate = tokenStore.shouldUpdateRefreshRate(600L)

            // Assert
            assertThat(shouldUpdate).isTrue()
        }

    @Test
    fun `shouldUpdateRefreshRate - given current refresh time exists and new is different but lower - returns true`() =
        runTest {
            // Arrange - Set up current refresh rate
            tokenStore.saveRefreshRateSeconds(1_000L)

            // Act
            val shouldUpdate = tokenStore.shouldUpdateRefreshRate(500L)

            // Assert
            assertThat(shouldUpdate).isTrue()
        }

    @Test
    fun `shouldUpdateRefreshRate - given current refresh time exists and new is same - returns false`() =
        runTest {
            // Arrange - Set up current refresh rate
            tokenStore.saveRefreshRateSeconds(300L)

            // Act
            val shouldUpdate = tokenStore.shouldUpdateRefreshRate(300L)

            // Assert
            assertThat(shouldUpdate).isFalse()
        }

    @Test
    fun `shouldUpdateRefreshRate - given current refresh time is null - returns false`() =
        runTest {
            // Arrange - Make sure no refresh rate exists
            tokenStore.clearRefreshRateSeconds()

            // Act
            val shouldUpdate = tokenStore.shouldUpdateRefreshRate(600L)

            // Assert
            assertThat(shouldUpdate).isFalse()
        }
}
