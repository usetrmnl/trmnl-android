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

// Load version properties
val versionPropsFile = file("version.properties")
val versionProps = java.util.Properties()

if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
} else {
    throw GradleException("version.properties file is missing! Cannot continue build.")
}

// Extract version info
val versionCode = versionProps.getProperty("VERSION_CODE", "0").toInt()
val versionName = versionProps.getProperty("VERSION_NAME", "0.0.0")
val gitTag = versionProps.getProperty("GIT_TAG", "v0.0.0")
val releaseNotes = versionProps.getProperty("RELEASE_NOTES", "")

// Make version info accessible to subprojects
ext {
    set("versionCode", versionCode)
    set("versionName", versionName)
    set("gitTag", gitTag)
    set("releaseNotes", releaseNotes)
}

// Task to build F-Droid APK
tasks.register("buildFDroid") {
    description = "Builds the F-Droid APK variant"
    dependsOn(":app:assembleFdroidRelease")
    
    doLast {
        println("F-Droid APK built successfully at: app/build/outputs/apk/fdroid/release/")
    }
}

// Task to build standard APK for GitHub workflows
tasks.register("buildStandard") {
    description = "Builds the standard APK variant"
    dependsOn(":app:assembleStandardRelease")
    
    doLast {
        println("Standard APK built successfully at: app/build/outputs/apk/standard/release/")
    }
}

// Task to verify all flavors build correctly
tasks.register("buildAllFlavors") {
    description = "Builds all flavor variants (Standard and F-Droid)"
    dependsOn("buildStandard", "buildFDroid")
    
    doLast {
        println("All flavor variants built successfully")
        println("Standard APK: app/build/outputs/apk/standard/release/")
        println("F-Droid APK: app/build/outputs/apk/fdroid/release/")
    }
}

// Task to validate version synchronization
tasks.register("validateVersionSync") {
    description = "Validates that version information is in sync across all files"
    
    doLast {
        val metadataFile = file("metadata/ink.trmnl.android.yml")
        val appBuildGradle = file("app/build.gradle.kts")
        val changelogFile = file("fastlane/metadata/android/en-US/changelogs/$versionCode.txt")
        
        var hasErrors = false
        
        // Check metadata file
        if (metadataFile.exists()) {
            val metadataContent = metadataFile.readText()
            if (!metadataContent.contains("versionCode: $versionCode") ||
                !metadataContent.contains("versionName: $versionName") ||
                !metadataContent.contains("commit: $gitTag")) {
                println("ERROR: metadata/ink.trmnl.android.yml has outdated version information")
                hasErrors = true
            }
        } else {
            println("WARNING: metadata/ink.trmnl.android.yml not found")
        }
        
        // Check app build.gradle
        if (appBuildGradle.exists()) {
            val buildContent = appBuildGradle.readText()
            val hasVersionCode = buildContent.contains("versionCode = $versionCode") || 
                                 buildContent.contains("versionCode = rootProject.extra[\"versionCode\"] as Int")
            val hasVersionName = buildContent.contains("versionName = \"$versionName\"") || 
                                 buildContent.contains("versionName = rootProject.extra[\"versionName\"] as String")
                                 
            if (!hasVersionCode || !hasVersionName) {
                println("ERROR: app/build.gradle.kts has outdated version information")
                hasErrors = true
            }
        } else {
            println("WARNING: app/build.gradle.kts not found")
        }
        
        // Check changelog file
        if (!changelogFile.exists()) {
            println("ERROR: Changelog file for version code $versionCode not found at: fastlane/metadata/android/en-US/changelogs/$versionCode.txt")
            hasErrors = true
        }
        
        if (hasErrors) {
            println("\nRun './gradlew syncVersionInfo' to update all files with version information from version.properties")
            throw GradleException("Version information is not in sync. See above errors.")
        } else {
            println("✓ All version information is in sync!")
        }
    }
}

// Task to synchronize version information across files
tasks.register("syncVersionInfo") {
    description = "Updates all files with version information from version.properties"
    
    doLast {
        // Update metadata/ink.trmnl.android.yml
        val metadataFile = file("metadata/ink.trmnl.android.yml")
        if (metadataFile.exists()) {
            var content = metadataFile.readText()
            content = content.replace(Regex("CurrentVersion: .+"), "CurrentVersion: $versionName")
            content = content.replace(Regex("CurrentVersionCode: \\d+"), "CurrentVersionCode: $versionCode")
            content = content.replace(Regex("versionName: .+"), "versionName: $versionName")
            content = content.replace(Regex("versionCode: \\d+"), "versionCode: $versionCode")
            content = content.replace(Regex("commit: .+"), "commit: $gitTag")
            metadataFile.writeText(content)
            println("✓ Updated metadata/ink.trmnl.android.yml with version $versionName ($versionCode)")
        }
        
        // Update app/build.gradle.kts
        val appBuildGradle = file("app/build.gradle.kts")
        if (appBuildGradle.exists()) {
            var content = appBuildGradle.readText()
            content = content.replace(Regex("versionCode = \\d+"), "versionCode = $versionCode")
            content = content.replace(Regex("versionName = \"[^\"]+\""), "versionName = \"$versionName\"")
            // Also update for new root project extra reference style
            content = content.replace(Regex("versionCode = rootProject.extra\\[\"versionCode\"\\] as Int"), "versionCode = rootProject.extra[\"versionCode\"] as Int")
            content = content.replace(Regex("versionName = rootProject.extra\\[\"versionName\"\\] as String"), "versionName = rootProject.extra[\"versionName\"] as String")
            appBuildGradle.writeText(content)
            println("✓ Updated app/build.gradle.kts with version $versionName ($versionCode)")
        }
        
        // Create/update changelog file
        val changelogDir = file("fastlane/metadata/android/en-US/changelogs")
        changelogDir.mkdirs()
        val changelogFile = file("fastlane/metadata/android/en-US/changelogs/$versionCode.txt")
        if (!changelogFile.exists()) {
            val formattedNotes = releaseNotes
                .split(",")
                .map { it.trim() }
                .joinToString("\n") { "- $it" }
            changelogFile.writeText("- Updated to the latest version ($versionName)\n$formattedNotes")
            println("✓ Created changelog file at fastlane/metadata/android/en-US/changelogs/$versionCode.txt")
        }
        
        println("\nVersion information synchronized to version $versionName ($versionCode)")
        println("Run './gradlew validateVersionSync' to verify all files are in sync")
    }
}

// Task to prepare a new release - combines sync and validate
tasks.register("prepareRelease") {
    description = "Prepares all files for a new release using version.properties information"
    dependsOn("syncVersionInfo", "validateVersionSync")
    
    doLast {
        println("\n✓ Release preparation for version $versionName ($versionCode) completed successfully!")
        println("Next steps:")
        println("  1. Commit the changes: git commit -am \"Prepare release $versionName\"")
        println("  2. Create a tag: git tag $gitTag")
        println("  3. Push changes: git push && git push origin $gitTag")
    }
}
