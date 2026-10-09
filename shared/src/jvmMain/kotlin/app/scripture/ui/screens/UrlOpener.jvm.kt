package app.scripture.ui.screens

import java.awt.Desktop
import java.net.URI

/** Desktop: hand the URL to the OS default browser via Swing's AWT bridge. */
actual fun openUrl(url: String): Boolean = runCatching {
    Desktop.getDesktop().browse(URI(url))
}.isSuccess