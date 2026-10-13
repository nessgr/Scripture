package house.gallery.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import house.gallery.GalleryApp

// The entire desktop "app": open a window and show the shared UI.
// This is the primary development loop — no emulator, instant launch.
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Gallery",
    ) {
        GalleryApp()
    }
}