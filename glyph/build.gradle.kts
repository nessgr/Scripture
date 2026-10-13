import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Word game core: the shared Glyph UI and logic, mirroring the level
// module's shape. All UI lives in commonMain and compiles to both targets.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

group = "house"

kotlin {
    android {
        namespace = "house.glyph.shared" // distinct from the :glyphAndroid app module
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Desktop (Windows/macOS/Linux) target — the fast iteration loop.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.designkit)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core) // StateFlow, CoroutineScope
            implementation(libs.kotlinx.serialization.json) // persisted settings
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}