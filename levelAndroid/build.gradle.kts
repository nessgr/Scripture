import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Thin Android entry point. AGP 9 has Kotlin support built in,
// so there is no separate Kotlin plugin here.
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

android {
    namespace = "house.level"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "house.level"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.level)
    implementation(libs.androidx.activity.compose)
}