import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.ksp)
    alias(libs.plugins.anvil)
}

// Load secret.properties file for local development
val secretPropsFile = rootProject.file("secret.properties")
if (secretPropsFile.exists()) {
    val secretProps = Properties()
    secretProps.load(secretPropsFile.inputStream())
    secretProps.forEach { key: Any, value: Any ->
        project.ext.set(key.toString(), value.toString())
    }
    logger.lifecycle("✅ Loaded `secret.properties` with ${secretProps.size} properties")
} else {
    logger.lifecycle("ℹ️ `secret.properties` file not found at ${secretPropsFile.absolutePath}")
}

android {
    namespace = "ink.trmnl.android"
    compileSdk = 35

    defaultConfig {
        // The application ID is the unique identifier for the app on the Play Store and other app stores.
        applicationId = "ink.trmnl.android"
        
        // Minimum SDK is Android 9.0 (Pie) with 94% Android devices coverage
        // Can't be lower than 28, See https://github.com/usetrmnl/trmnl-android/pull/56
        minSdk = 28

        // See https://apilevels.com/
        targetSdk = 35 // Android 15.0 (Vanilla Ice Cream)
        
        // App versioning is scattered across multiple files.
        // 👇🏽 Use the following workflow to update versions everywhere automatically ♻️
        // https://github.com/usetrmnl/trmnl-android/actions/workflows/version-management.yml
        versionCode = 18
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootProject.projectDir}/keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        
        create("release") {
            // Production keystore for release builds
            // The keystore file should be decoded from KEYSTORE_BASE64 secret in CI/CD
            storeFile = file("${rootProject.projectDir}/keystore/trmnl-app-release.keystore")

            // Values come from CI/CD secrets or local secret.properties file
            // Note: keyPassword is set to the same value as storePassword because this PKCS12 keystore
            // requires the store password to be used for both store and key access
            val storePass = System.getenv("KEYSTORE_PASSWORD") ?: project.findProperty("KEYSTORE_PASSWORD") as String?
            val keyAliasValue = System.getenv("KEY_ALIAS") ?: project.findProperty("KEY_ALIAS") as String?
            
            storePassword = storePass
            keyAlias = keyAliasValue
            keyPassword = storePass
        }
    }

    buildTypes {
        release {
            // Always force `USE_FAKE_API` to `false` for release builds
            // See https://github.com/usetrmnl/trmnl-android/issues/16
            buildConfigField("Boolean", "USE_FAKE_API", "false")

            // Enables code shrinking, obfuscation, and optimization
            isMinifyEnabled = true

            // Enables resource shrinking, which is performed by the Android Gradle plugin.
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // CI: Uses environment variables from secrets
            // Local: Uses `secret.properties` file (automatically loaded by Gradle)
            signingConfig = signingConfigs.getByName("release")
        }
        
        debug {
            // Allow developers to configure this value for debug builds
            // Use fake API response for local development and testing purposes.
            buildConfigField("Boolean", "USE_FAKE_API", "true")

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.window)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.circuit.codegen.annotations)
    implementation(libs.circuit.foundation)
    implementation(libs.circuit.overlay)
    implementation(libs.circuitx.android)
    implementation(libs.circuitx.effects)
    implementation(libs.circuitx.gestureNav)
    implementation(libs.circuitx.overlays)
    implementation(libs.androidx.compose.materialWindow)
    implementation(libs.androidx.adaptive)
    implementation(libs.core.ktx)
    ksp(libs.circuit.codegen)

    implementation(libs.dagger)
    // Dagger KSP support is in Alpha, not available yet. Using KAPT for now.
    // https://dagger.dev/dev-guide/ksp.html
    kapt(libs.dagger.compiler)

    implementation(libs.anvil.annotations)
    implementation(libs.anvil.annotations.optional)

    // Timber
    implementation(libs.timber)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.moshi)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Moshi
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // EitherNet
    implementation(libs.eithernet)
    implementation(libs.eithernet.integration.retrofit)

    // Testing
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.truth)
}

ksp {
    // Anvil-KSP
    arg("anvil-ksp-extraContributingAnnotations", "com.slack.circuit.codegen.annotations.CircuitInject")
    // kotlin-inject-anvil (requires 0.0.3+)
    arg("kotlin-inject-anvil-contributing-annotations", "com.slack.circuit.codegen.annotations.CircuitInject")
}

// Enable dynamic agent loading for tests needed by MockK
// https://github.com/hossain-khan/android-trmnl-display/pull/106#issuecomment-2826350990
tasks.withType<Test> {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}
