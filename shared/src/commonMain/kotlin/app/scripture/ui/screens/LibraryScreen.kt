package app.scripture.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.scripture.ScriptureStore
import app.scripture.domain.LibraryFilter
import app.scripture.domain.Verse
import app.scripture.domain.allTags
import app.scripture.domain.filterVerses
import app.scripture.domain.isDue
import app.scripture.ui.designsystem.EmptyState
import app.scripture.ui.designsystem.PrimaryButton
import app.scripture.ui.designsystem.ScreenScaffold
import app.scripture.ui.designsystem.Spacing
import app.scripture.ui.designsystem.TagChip
import app.scripture.ui.designsystem.VerseCard
import app.scripture.ui.designsystem.label
import app.scripture.ui.designsystem.reference

/** Home: the verse library, searchable and tag-filterable, plus review entry. */
@Composable
fun LibraryScreen(
    store: ScriptureStore,
    onAddVerse: () -> Unit,
    onStartReview: () -> Unit,
    onOpenVerse: (String) -> Unit,
    onImport: () -> Unit,
    onAbout: () -> Unit,
) {
    val verses by store.verses.collectAsState()
    val today = store.todayEpochDay()
    val dueCount = verses.count { isDue(it.progress, today) }

    // Saved with rememberSaveable so a process death mid-search keeps the query.
    // Empty string is the "no tag selected" sentinel, not null — simpler to save.
    var query by rememberSaveable { mutableStateOf("") }
    var activeTag by rememberSaveable { mutableStateOf("") }
    val filter = LibraryFilter(query = query, tag = activeTag.ifEmpty { null })
    val visible = remember(verses, filter) { filterVerses(verses, filter) }
    val tags = remember(verses) { allTags(verses) }

    ScreenScaffold(title = "Scripture") {
        if (verses.isEmpty()) {
            EmptyState(
                message = "No verses yet",
                hint = "Add a verse you want to carry with you.",
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton("Add your first verse", onClick = onAddVerse, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            ) { Text("Import from a file") }
            OutlinedButton(
                onClick = onAbout,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            ) { Text("About this app") }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
            ) {
                Text(
                    // Counts stay unfiltered: "Start review" grades every due
                    // verse, whatever the list is narrowed to.
                    text = if (filter.isActive) "${visible.size} of ${verses.size} verses match"
                           else "$dueCount due today · ${verses.size} total",
                    style = MaterialTheme.label,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedButton(onClick = onImport) { Text("Import") }
                    OutlinedButton(onClick = onAddVerse) { Text("Add verse") }
                    OutlinedButton(onClick = onAbout) { Text("About") }
                }
            }

            if (dueCount > 0 && !filter.isActive) {
                PrimaryButton(
                    text = "Start review ($dueCount)",
                    onClick = onStartReview,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                placeholder = { Text("Reference or text…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
            )

            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = Spacing.md),
                ) {
                    TagChip(
                        label = "All",
                        selected = activeTag.isEmpty(),
                        onClick = { activeTag = "" },
                    )
                    tags.forEach { tag ->
                        TagChip(
                            label = tag,
                            selected = activeTag == tag,
                            onClick = { activeTag = if (activeTag == tag) "" else tag },
                        )
                    }
                }
            }

            if (visible.isEmpty()) {
                EmptyState("No matches", "Try a different search or clear the filters.")
            } else {
                val sections = remember(visible) { verseSections(visible) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    sections.forEach { section ->
                        section.title?.let { title ->
                            // Keyed by the section's first verse so two runs of
                            // the same title can't collide in the lazy list.
                            item(key = "heading-${section.verses.first().id}") {
                                Text(
                                    title.uppercase(),
                                    style = MaterialTheme.reference,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = Spacing.sm),
                                )
                            }
                        }
                        items(section.verses, key = { it.id }) { verse ->
                            VerseCard(verse, onClick = { onOpenVerse(verse.id) })
                        }
                    }
                }
            }
        }
    }
}

private data class VerseSection(val title: String?, val verses: List<Verse>)

/**
 * Groups verses into headed runs: consecutive verses that carry the same
 * collection title (the title of the file they were imported from).
 * Verses added by hand have no title and sit between runs, unheaded.
 */
private fun verseSections(verses: List<Verse>): List<VerseSection> {
    val sections = mutableListOf<VerseSection>()
    verses.forEach { verse ->
        val last = sections.lastOrNull()
        if (last != null && last.title == verse.collection) {
            sections[sections.lastIndex] = last.copy(verses = last.verses + verse)
        } else {
            sections.add(VerseSection(verse.collection, listOf(verse)))
        }
    }
    return sections
}