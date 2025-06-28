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
        versionCode = 16
        versionName = "1.9.5"

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
            // Check if we're running in CI environment
            val isRunningOnCI = System.getenv("CI") == "true" || project.hasProperty("CI")
            println("🔍 CI Environment Variable: $isRunningOnCI")
            
            if (isRunningOnCI) {
                // Try multiple sources for the keystore path
                var keystorePath: String? = System.getenv("KEYSTORE_PATH")
                if (keystorePath.isNullOrEmpty() && project.hasProperty("KEYSTORE_PATH")) {
                    keystorePath = project.property("KEYSTORE_PATH") as String
                }
                
                println("🔍 KEYSTORE_PATH: ${keystorePath ?: "not set"}")
                println("🔍 KEYSTORE_PATH exists: ${if (keystorePath != null) file(keystorePath).exists() else false}")
                
                // Check for explicit keystore file
                val explicitKeystoreFile = file("$rootDir/release.keystore")
                if (explicitKeystoreFile.exists()) {
                    println("🔍 Found explicit keystore at ${explicitKeystoreFile.absolutePath}")
                }
                
                if ((keystorePath != null && file(keystorePath).exists()) || explicitKeystoreFile.exists()) {
                    val actualKeystoreFile = if (keystorePath != null && file(keystorePath).exists()) {
                        file(keystorePath)
                    } else {
                        explicitKeystoreFile
                    }
                    
                    // Print keystore file info
                    println("🔍 Using keystore: ${actualKeystoreFile.absolutePath}")
                    println("🔍 Keystore file size: ${actualKeystoreFile.length()} bytes")
                    
                    // Try multiple sources for credentials
                    var storePass = System.getenv("KEYSTORE_PASSWORD")
                    if (storePass.isNullOrEmpty() && project.hasProperty("KEYSTORE_PASSWORD")) {
                        storePass = project.property("KEYSTORE_PASSWORD") as String
                    }
                    
                    var keyAliasEnv = System.getenv("KEY_ALIAS")
                    if (keyAliasEnv.isNullOrEmpty() && project.hasProperty("KEY_ALIAS")) {
                        keyAliasEnv = project.property("KEY_ALIAS") as String
                    }
                    
                    var keyPassEnv = System.getenv("KEY_PASSWORD")
                    if (keyPassEnv.isNullOrEmpty() && project.hasProperty("KEY_PASSWORD")) {
                        keyPassEnv = project.property("KEY_PASSWORD") as String
                    }
                    
                    println("🔍 KEYSTORE_PASSWORD is set: ${!storePass.isNullOrEmpty()}")
                    println("🔍 KEY_ALIAS is set: ${!keyAliasEnv.isNullOrEmpty()}")
                    println("🔍 KEY_PASSWORD is set: ${!keyPassEnv.isNullOrEmpty()}")
                    
                    // Only use the CI keystore if all required credentials are provided
                    if (!storePass.isNullOrEmpty() && !keyAliasEnv.isNullOrEmpty() && !keyPassEnv.isNullOrEmpty()) {
                        try {
                            storeFile = actualKeystoreFile
                            storePassword = storePass
                            keyAlias = keyAliasEnv
                            keyPassword = keyPassEnv
                            println("✅ Configured signing with keystore: ${actualKeystoreFile.absolutePath}")
                            println("✅ StoreFile path: ${storeFile?.absolutePath}")
                            println("✅ KeyAlias: $keyAlias")
                        } catch (e: Exception) {
                            println("❌ Error setting up signing: ${e.message}")
                            // Fallback to debug keystore
                            storeFile = file("${rootProject.projectDir}/keystore/debug.keystore")
                            storePassword = "android"
                            keyAlias = "androiddebugkey"
                            keyPassword = "android"
                        }
                    } else {
                        println("⚠️ Missing required signing credentials. Fallback to debug keystore.")
                        storeFile = file("${rootProject.projectDir}/keystore/debug.keystore")
                        storePassword = "android"
                        keyAlias = "androiddebugkey"
                        keyPassword = "android"
                    }
                } else {
                    println("⚠️ Keystore not found. Fallback to debug keystore.")
                    storeFile = file("${rootProject.projectDir}/keystore/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            } else {
                // For local development, use the debug keystore
                println("ℹ️ Using debug keystore for release builds (local development)")
                storeFile = file("${rootProject.projectDir}/keystore/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
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
        }
        
        debug {
            // Allow developers to configure this value for debug builds
            // Use fake API response for local development and testing purposes.
            buildConfigField("Boolean", "USE_FAKE_API", "true")

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Add product flavors for F-Droid compatibility
    flavorDimensions += "store"
    productFlavors {
        create("standard") {
            dimension = "store"
            // Standard flavor with all features
            buildConfigField("Boolean", "FDROID_BUILD", "false")
            
            // Apply signing for standard flavor
            signingConfig = signingConfigs.getByName("release")
        }
        
        create("fdroid") {
            dimension = "store"
            // F-Droid specific configuration
            // No non-free dependencies, but still signed
            buildConfigField("Boolean", "FDROID_BUILD", "true")
            
            // Apply the same signing configuration for F-Droid builds
            signingConfig = signingConfigs.getByName("release")
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
