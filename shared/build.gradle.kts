import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The heart of the app: ALL domain logic, data, and UI live in `commonMain`
// and compile to both Android and the desktop JVM. Platform-specific code
// is limited to one small `Platform` file per target.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary) // new AGP-9-era Android target for shared code
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// The generated accessors for composeResources (Res.font.* etc.) live in
// the package `<group>.<module-name>.generated.resources`.
group = "app.scripture"

kotlin {
    android {
        namespace = "app.scripture.shared"
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
            // Compose accessors come from the composeMultiplatform plugin
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core) // StateFlow, CoroutineScope
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}