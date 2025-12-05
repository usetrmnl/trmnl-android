package ink.trmnl.android.network.model

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test

/**
 * Unit tests for [TrmnlModelsResponse] and [TrmnlDeviceModel].
 *
 * These tests verify JSON parsing of the /api/models endpoint response.
 */
class TrmnlModelsResponseTest {
    private val moshi =
        Moshi
            .Builder()
            .build()

    private val modelsAdapter = moshi.adapter(TrmnlModelsResponse::class.java)

    @Test
    fun `parse models response from JSON successfully`() {
        // Arrange - Load the test JSON from resources
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("models_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Assert that we successfully loaded the JSON
        assertThat(json).isNotNull()

        // Act
        val response = modelsAdapter.fromJson(json!!)

        // Assert
        assertThat(response).isNotNull()
        assertThat(response?.data).isNotNull()
        assertThat(response?.data).hasSize(22)
    }

    @Test
    fun `parse first device model correctly`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("models_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = modelsAdapter.fromJson(json!!)
        val firstModel = response?.data?.first()

        // Assert - Verify TRMNL X (v2) model
        assertThat(firstModel).isNotNull()
        assertThat(firstModel?.name).isEqualTo("v2")
        assertThat(firstModel?.label).isEqualTo("TRMNL X")
        assertThat(firstModel?.description).isEqualTo("TRMNL X")
        assertThat(firstModel?.width).isEqualTo(1872)
        assertThat(firstModel?.height).isEqualTo(1404)
        assertThat(firstModel?.colors).isEqualTo(16)
        assertThat(firstModel?.bitDepth).isEqualTo(4)
        assertThat(firstModel?.scaleFactor).isEqualTo(1.8)
        assertThat(firstModel?.rotation).isEqualTo(0)
        assertThat(firstModel?.mimeType).isEqualTo("image/png")
        assertThat(firstModel?.offsetX).isEqualTo(0)
        assertThat(firstModel?.offsetY).isEqualTo(0)
        assertThat(firstModel?.publishedAt).isEqualTo("2024-01-01T00:00:00.000Z")
        assertThat(firstModel?.kind).isEqualTo("trmnl")
        assertThat(firstModel?.paletteIds).containsExactly("bw", "gray-4", "gray-16")
    }

    @Test
    fun `parse TRMNL OG model correctly`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("models_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = modelsAdapter.fromJson(json!!)
        val ogModel = response?.data?.find { it.name == "og_png" }

        // Assert
        assertThat(ogModel).isNotNull()
        assertThat(ogModel?.name).isEqualTo("og_png")
        assertThat(ogModel?.label).isEqualTo("TRMNL OG (1-bit)")
        assertThat(ogModel?.width).isEqualTo(800)
        assertThat(ogModel?.height).isEqualTo(480)
        assertThat(ogModel?.colors).isEqualTo(2)
        assertThat(ogModel?.bitDepth).isEqualTo(1)
        assertThat(ogModel?.scaleFactor).isEqualTo(1.0)
        assertThat(ogModel?.kind).isEqualTo("trmnl")
        assertThat(ogModel?.paletteIds).containsExactly("bw")
    }

    @Test
    fun `parse Kindle model with offset correctly`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("models_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = modelsAdapter.fromJson(json!!)
        val kindleModel = response?.data?.find { it.name == "amazon_kindle_2024" }

        // Assert
        assertThat(kindleModel).isNotNull()
        assertThat(kindleModel?.name).isEqualTo("amazon_kindle_2024")
        assertThat(kindleModel?.label).isEqualTo("Amazon Kindle 2024")
        assertThat(kindleModel?.width).isEqualTo(1400)
        assertThat(kindleModel?.height).isEqualTo(840)
        assertThat(kindleModel?.colors).isEqualTo(256)
        assertThat(kindleModel?.bitDepth).isEqualTo(8)
        assertThat(kindleModel?.scaleFactor).isEqualTo(1.75)
        assertThat(kindleModel?.rotation).isEqualTo(90)
        assertThat(kindleModel?.offsetX).isEqualTo(75)
        assertThat(kindleModel?.offsetY).isEqualTo(25)
        assertThat(kindleModel?.kind).isEqualTo("kindle")
        assertThat(kindleModel?.paletteIds).containsExactly("gray-256")
    }

    @Test
    fun `parse Tidbyt model with webp mime type correctly`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("models_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = modelsAdapter.fromJson(json!!)
        val tidbytModel = response?.data?.find { it.name == "tidbyt" }

        // Assert
        assertThat(tidbytModel).isNotNull()
        assertThat(tidbytModel?.name).isEqualTo("tidbyt")
        assertThat(tidbytModel?.label).isEqualTo("Tidbyt")
        assertThat(tidbytModel?.width).isEqualTo(64)
        assertThat(tidbytModel?.height).isEqualTo(32)
        assertThat(tidbytModel?.colors).isEqualTo(16777216)
        assertThat(tidbytModel?.bitDepth).isEqualTo(24)
        assertThat(tidbytModel?.mimeType).isEqualTo("image/webp")
        assertThat(tidbytModel?.kind).isEqualTo("tidbyt")
        assertThat(tidbytModel?.paletteIds).containsExactly("color-24bit")
    }

    @Test
    fun `parse BYOD model correctly`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("models_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = modelsAdapter.fromJson(json!!)
        val byodModel = response?.data?.find { it.name == "inkplate_10" }

        // Assert
        assertThat(byodModel).isNotNull()
        assertThat(byodModel?.name).isEqualTo("inkplate_10")
        assertThat(byodModel?.label).isEqualTo("Inkplate 10")
        assertThat(byodModel?.kind).isEqualTo("byod")
        assertThat(byodModel?.paletteIds).containsExactly("bw", "gray-4")
    }

    @Test
    fun `verify all device kinds are present`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("models_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        // Act
        val response = modelsAdapter.fromJson(json!!)
        val kinds = response?.data?.map { it.kind }?.distinct()

        // Assert - Verify we have models for all device kinds
        assertThat(kinds).containsExactly("trmnl", "kindle", "byod", "tidbyt")
    }

    @Test
    fun `parse single device model from JSON`() {
        // Arrange
        val singleModelJson =
            """
            {
              "name": "test_model",
              "label": "Test Model",
              "description": "Test Description",
              "width": 1000,
              "height": 600,
              "colors": 4,
              "bit_depth": 2,
              "scale_factor": 1.5,
              "rotation": 90,
              "mime_type": "image/png",
              "offset_x": 10,
              "offset_y": 20,
              "published_at": "2024-01-01T00:00:00.000Z",
              "kind": "test",
              "palette_ids": ["test-palette"]
            }
            """.trimIndent()

        val deviceModelAdapter = moshi.adapter(TrmnlDeviceModel::class.java)

        // Act
        val model = deviceModelAdapter.fromJson(singleModelJson)

        // Assert
        assertThat(model).isNotNull()
        assertThat(model?.name).isEqualTo("test_model")
        assertThat(model?.label).isEqualTo("Test Model")
        assertThat(model?.description).isEqualTo("Test Description")
        assertThat(model?.width).isEqualTo(1000)
        assertThat(model?.height).isEqualTo(600)
        assertThat(model?.colors).isEqualTo(4)
        assertThat(model?.bitDepth).isEqualTo(2)
        assertThat(model?.scaleFactor).isEqualTo(1.5)
        assertThat(model?.rotation).isEqualTo(90)
        assertThat(model?.mimeType).isEqualTo("image/png")
        assertThat(model?.offsetX).isEqualTo(10)
        assertThat(model?.offsetY).isEqualTo(20)
        assertThat(model?.publishedAt).isEqualTo("2024-01-01T00:00:00.000Z")
        assertThat(model?.kind).isEqualTo("test")
        assertThat(model?.paletteIds).containsExactly("test-palette")
    }

    @Test
    fun `verify all expected models are present`() {
        // Arrange
        val json =
            javaClass.classLoader
                ?.getResourceAsStream("models_response.json")
                ?.bufferedReader()
                ?.use { it.readText() }

        val expectedModelNames =
            listOf(
                "v2",
                "og_png",
                "amazon_kindle_2024",
                "amazon_kindle_paperwhite_6th_gen",
                "amazon_kindle_paperwhite_7th_gen",
                "inkplate_10",
                "amazon_kindle_7",
                "inky_impression_7_3",
                "kobo_libra_2",
                "amazon_kindle_oasis_2",
                "og_plus",
                "kobo_aura_one",
                "kobo_aura_hd",
                "inky_impression_13_3",
                "m5_paper_s3",
                "amazon_kindle_scribe",
                "seeed_e1001",
                "seeed_e1002",
                "waveshare_4_26",
                "waveshare_7_5_bw",
                "tidbyt",
                "generic_16_9",
            )

        // Act
        val response = modelsAdapter.fromJson(json!!)
        val modelNames = response?.data?.map { it.name }

        // Assert
        assertThat(modelNames).containsExactlyElementsIn(expectedModelNames)
    }
}
