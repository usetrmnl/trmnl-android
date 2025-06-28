// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Applies the Android application plugin.
    // Project: https://developer.android.com/build
    alias(libs.plugins.android.application) apply false

    // Applies the Kotlin Android plugin.
    // Project: https://kotlinlang.org/docs/android-overview.html
    alias(libs.plugins.kotlin.android) apply false

    // Applies the Kotlin Compose plugin.
    // Project: https://developer.android.com/jetpack/compose
    alias(libs.plugins.kotlin.compose) apply false

    // Applies the Kotlin Parcelize plugin.
    // Project: https://developer.android.com/kotlin/parcelize
    alias(libs.plugins.kotlin.parcelize) apply false

    // Applies the Kotlin KAPT (Kotlin Annotation Processing Tool) plugin.
    // Project: https://kotlinlang.org/docs/kapt.html
    alias(libs.plugins.kotlin.kapt) apply false

    // Applies the Kotlin Symbol Processing (KSP) plugin.
    // Project: https://github.com/google/ksp
    alias(libs.plugins.ksp) apply false

    // Applies the Anvil plugin for Dagger dependency injection.
    // Project: https://github.com/square/anvil
    // Also see: https://github.com/ZacSweers/anvil/blob/main/FORK.md
    alias(libs.plugins.anvil) apply false
}
