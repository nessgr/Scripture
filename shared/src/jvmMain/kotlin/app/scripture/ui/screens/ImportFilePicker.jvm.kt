package app.scripture.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Desktop picker: Swing's JFileChooser. Compose Desktop's Main dispatcher
 * is the AWT event thread, so the modal dialog is at home here. JDK Swing
 * over a compose file-dialog API: guaranteed present on every JVM, and its
 * file filter actually works on Windows (FileDialog's does not).
 */
@Composable
actual fun rememberImportFilePicker(): ImportFilePicker = remember {
    object : ImportFilePicker {
        override suspend fun pick(): PickOutcome {
            val chooser = JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("JSON / text", "json", "txt")
                isMultiSelectionEnabled = false
            }
            return withContext(Dispatchers.Main) {
                if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                    PickOutcome.Cancelled
                } else {
                    readFile(chooser.selectedFile)
                }
            }
        }

        private fun readFile(file: File?): PickOutcome =
            runCatching { file?.readText() }.getOrNull()?.let(PickOutcome::Read)
                ?: PickOutcome.Unreadable
    }
}