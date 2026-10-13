// Thin desktop entry point — a plain JVM module that renders the shared UI.
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.level)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "house.level.desktop.MainKt"
    }
}