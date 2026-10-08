package app.scripture.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android picker: the system document picker (SAF — no storage permission
 * needed), read once via the content resolver. `pick()` suspends on a
 * deferred that the launcher callback completes off the UI thread.
 */
@Composable
actual fun rememberImportFilePicker(): ImportFilePicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<CompletableDeferred<PickOutcome>?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val deferred = pending ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            deferred.complete(PickOutcome.Cancelled)
        } else {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                deferred.complete(text?.let(PickOutcome::Read) ?: PickOutcome.Unreadable)
            }
        }
    }

    return remember {
        object : ImportFilePicker {
            override suspend fun pick(): PickOutcome {
                // A fresh deferred per pick; supersede any in-flight one so
                // nothing ever awaits forever.
                pending?.complete(PickOutcome.Cancelled)
                val deferred = CompletableDeferred<PickOutcome>()
                pending = deferred
                // OpenDocument shows only these MIME types; octet-stream
                // covers .json files saved without a MIME mapping.
                launcher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                return deferred.await()
            }
        }
    }
}