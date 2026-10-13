import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The house design system: theme, tokens, and the components every house
// app wears. Knows nothing about any specific app's domain.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// The generated accessors for composeResources (Res.font.* etc.) live in
// the package `<group>.<module-name>.generated.resources` — here
// house.designkit.generated.resources. They are imported only inside
// Theme.kt and never leak into the kit's public API.
group = "house"

kotlin {
    android {
        namespace = "house.designkit"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Desktop (Windows/macOS/Linux) target — must match :shared's JVM target,
    // whose classpath this joins.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}