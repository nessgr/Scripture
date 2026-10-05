package app.scripture.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.scripture.App

// The entire desktop "app": open a window and show the shared UI.
// This is the primary development loop — no emulator, instant launch.
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Scripture",
    ) {
        App()
    }
}