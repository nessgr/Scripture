package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import app.scripture.ScriptureStore
import app.scripture.domain.isDue
import app.scripture.ui.designsystem.EmptyState
import app.scripture.ui.designsystem.PrimaryButton
import app.scripture.ui.designsystem.ScreenScaffold
import app.scripture.ui.designsystem.Spacing
import app.scripture.ui.designsystem.VerseCard
import app.scripture.ui.designsystem.label

/** Home: the verse library plus entry points to add and review. */
@Composable
fun LibraryScreen(
    store: ScriptureStore,
    onAddVerse: () -> Unit,
    onStartReview: () -> Unit,
) {
    val verses by store.verses.collectAsState()
    val today = store.todayEpochDay()
    val dueCount = verses.count { isDue(it.progress, today) }

    ScreenScaffold(title = "Scripture") {
        if (verses.isEmpty()) {
            EmptyState(
                message = "No verses yet",
                hint = "Add a verse you want to carry with you.",
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton("Add your first verse", onClick = onAddVerse, modifier = Modifier.fillMaxWidth())
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
            ) {
                Text(
                    text = "$dueCount due today · ${verses.size} total",
                    style = MaterialTheme.label,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(onClick = onAddVerse) { Text("Add verse") }
            }

            if (dueCount > 0) {
                PrimaryButton(
                    text = "Start review ($dueCount)",
                    onClick = onStartReview,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                items(verses, key = { it.id }) { verse ->
                    VerseCard(verse)
                }
            }
        }
    }
}