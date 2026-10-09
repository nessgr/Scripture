package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import app.scripture.ui.designsystem.PrimaryButton
import app.scripture.ui.designsystem.ScreenScaffold
import app.scripture.ui.designsystem.Spacing
import app.scripture.ui.designsystem.body
import app.scripture.ui.designsystem.label
import app.scripture.ui.designsystem.scriptureColor
import app.scripture.ui.designsystem.scriptureDisplay
import app.scripture.ui.designsystem.title

/**
 * The About page: what this app is, the promise it makes (free, safe,
 * offline), the author's other tools, and a way to say thanks.
 */

/** Where the "buy me a coffee" button points. */
private const val supportUrl =
    "https://mc.buymeacoffee.com/links/syVbEPSxvWdRBDdDJhXMSUsvcfAEAaPkdDujxhMHWkWlVFXfzPswiACqBhwBifAayfMkFAV/2876893?link=gness"

/** The author's other apps — name to page. Grows one line at a time. */
private val otherApps: List<Pair<String, String>> = emptyList()

/** The promises, one per line, rendered as a plain bulleted list. */
private val promises = listOf(
    "• No accounts, no ads, no tracking.",
    "• Works fully offline.",
    "• Your library lives only on your device — nothing ever leaves it.",
    "• Free, today and always.",
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    var linkFailed by remember { mutableStateOf(false) }

    ScreenScaffold(title = "About", onBack = onBack, scrollable = true) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text(
                "Scripture",
                style = MaterialTheme.scriptureDisplay,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "A companion for memorizing scripture. The verses you add are " +
                    "woven into daily review — five drills that ask for more of " +
                    "each verse as you come to know it, scheduled by spaced repetition.",
                style = MaterialTheme.body,
                color = MaterialTheme.scriptureColor,
            )

            Text("Free and safe", style = MaterialTheme.title)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                promises.forEach { promise ->
                    Text(promise, style = MaterialTheme.body)
                }
            }

            Text("Other tools", style = MaterialTheme.title)
            if (otherApps.isEmpty()) {
                Text(
                    "Check back — more apps are on the way.",
                    style = MaterialTheme.body,
                )
            } else {
                otherApps.forEach { (name, url) ->
                    OutlinedButton(onClick = { openUrl(url) }, modifier = Modifier.fillMaxWidth()) {
                        Text(name)
                    }
                }
            }

            Text("Support this app", style = MaterialTheme.title)
            Text(
                "If Scripture helps you, you can buy me a cup of coffee — or a small island.",
                style = MaterialTheme.body,
            )
            PrimaryButton(
                text = "Buy me a coffee",
                onClick = { linkFailed = !openUrl(supportUrl) },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )
            if (linkFailed) {
                Text(
                    "Couldn't open a browser — copy this address instead:",
                    style = MaterialTheme.label,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            // Always visible: the raw address, copyable even when the
            // button works — a link is a promise, not a mystery.
            Text(
                supportUrl,
                style = MaterialTheme.label.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}