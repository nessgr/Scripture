import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Starter app shell: consumes the house design kit, will grow into a real
// app. All UI lives in commonMain and compiles to both targets, mirroring
// the shared module's shape.
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
        namespace = "house.level.shared" // distinct from the :levelAndroid app module
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