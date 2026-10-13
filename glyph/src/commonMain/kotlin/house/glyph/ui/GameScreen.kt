package house.glyph.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import house.design.PrimaryButton
import house.design.ScreenScaffold
import house.design.Spacing
import house.design.ThemeToggle
import house.design.archShape
import house.design.emberColor
import house.design.hintColor
import house.design.inkColor
import house.design.label
import house.design.readingDisplay
import house.design.reference
import house.design.title
import house.glyph.GameStore
import house.glyph.SettingsStore
import house.glyph.StatsStore
import house.glyph.domain.Game
import house.glyph.domain.GuessOutcome
import house.glyph.domain.GuessState
import house.glyph.domain.LetterMark
import house.glyph.domain.MAX_GUESSES
import house.glyph.domain.WORD_LENGTH
import house.glyph.domain.bestMark
import kotlinx.coroutines.delay

/** The board: fixed square tiles with breathing room between them. */
private val TILE_SIZE = 56.dp
private val REVEAL_STAGGER_MS = 160L
private val FLIP_HALF_MS = 180

/** Hardware letter keys to their letters, for typing on the board with a real keyboard. */
private val letterKeys = mapOf(
    Key.A to 'a', Key.B to 'b', Key.C to 'c', Key.D to 'd', Key.E to 'e',
    Key.F to 'f', Key.G to 'g', Key.H to 'h', Key.I to 'i', Key.J to 'j',
    Key.K to 'k', Key.L to 'l', Key.M to 'm', Key.N to 'n', Key.O to 'o',
    Key.P to 'p', Key.Q to 'q', Key.R to 'r', Key.S to 's', Key.T to 't',
    Key.U to 'u', Key.V to 'v', Key.W to 'w', Key.X to 'x', Key.Y to 'y',
    Key.Z to 'z',
)

/**
 * The instrument: a grid of engraved arch tiles, the on-screen keyboard
 * underneath, and — when a round ends — the word, the streak, and the
 * way to start over. Hardware keyboards work too; the screen grabs
 * focus so typing anywhere lands on the board.
 */
@Composable
fun GameScreen(
    settings: SettingsStore,
    games: GameStore,
    stats: StatsStore,
    onHelp: () -> Unit,
) {
    val game by games.game.collectAsState()
    val record by stats.stats.collectAsState()
    var draft by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    // Rows already played their reveal; everything at or below this shows settled.
    var revealedCount by remember { mutableStateOf(game.guesses.size) }
    val focus = remember { FocusRequester() }

    fun type(letter: Char) {
        if (game.state == GuessState.PLAYING && draft.length < WORD_LENGTH) draft += letter
        focus.requestFocus()
    }

    fun back() {
        if (draft.isNotEmpty()) draft = draft.dropLast(1)
        focus.requestFocus()
    }

    fun enter() {
        when (val outcome = games.submit(draft)) {
            is GuessOutcome.Accepted -> draft = ""
            is GuessOutcome.Rejected -> notice = when (outcome.reason) {
                house.glyph.domain.RejectReason.WRONG_LENGTH ->
                    if (draft.isEmpty()) "Type a word first" else "Words are five letters"
                house.glyph.domain.RejectReason.NOT_A_WORD -> "Not in the dictionary"
                house.glyph.domain.RejectReason.GAME_OVER -> null
            }
        }
        focus.requestFocus()
    }

    // A new word wipes the half-typed one; nobody wants a carried-over draft.
    LaunchedEffect(game.answer) { draft = "" }

    // Let the newest row finish its cascade before it settles.
    LaunchedEffect(game.guesses.size) {
        if (game.guesses.size > revealedCount) {
            delay(REVEAL_STAGGER_MS * WORD_LENGTH + FLIP_HALF_MS * 2)
            revealedCount = game.guesses.size
        }
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2200)
            notice = null
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    ScreenScaffold(
        title = "Glyph",
        scrollable = true,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onHelp) { Text("Help") }
                ThemeToggle(choice = settings.choice, onChoice = { settings.setChoice(it) })
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            modifier = Modifier
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.Backspace -> { back(); true }
                        event.key == Key.Enter || event.key == Key.NumPadEnter -> { enter(); true }
                        else -> {
                            // Named key constants are the portable signal —
                            // keycodes and character codes differ by platform.
                            val typed = letterKeys[event.key]
                            if (typed != null) {
                                type(typed)
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
                .focusable(),
        ) {
            Board(game, draft, revealedCount)

            // Fixed height so the layout doesn't jump when a notice appears.
            Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                if (notice != null) {
                    Text(notice!!, style = MaterialTheme.reference, color = MaterialTheme.hintColor)
                }
            }

            if (game.state != GuessState.PLAYING) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (game.state == GuessState.WON) {
                                "Got it in ${game.guesses.size}."
                            } else {
                                "It was ${game.answer.uppercase()}."
                            },
                            style = MaterialTheme.title,
                            color = MaterialTheme.inkColor,
                        )
                        Text(
                            "Streak ${record.streak} · best ${record.bestStreak}",
                            style = MaterialTheme.reference,
                            color = MaterialTheme.hintColor,
                        )
                    }
                    PrimaryButton("New word", onClick = { games.newGame() })
                }
            }

            LetterKeyboard(
                marks = buildMap {
                    for (letter in 'a'..'z') bestMark(game.guesses, letter)?.let { put(letter, it) }
                },
                enabled = game.state == GuessState.PLAYING,
                onKey = ::type,
                onSubmit = ::enter,
                onBackspace = ::back,
            )

            StatsFooter(record.played, record.wins, record.distribution)
        }
    }
}

@Composable
private fun Board(game: Game, draft: String, revealedCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        repeat(MAX_GUESSES) { row ->
            val guess = game.guesses.getOrNull(row)
            val animate = row == game.guesses.lastIndex && game.guesses.size > revealedCount
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                repeat(WORD_LENGTH) { col ->
                    // Keyed by the word too, so a tile that gets new content
                    // restarts its reveal animation from scratch.
                    key("$row-${guess?.word ?: "draft"}-$col") {
                        RevealTile(
                            letter = guess?.word?.getOrNull(col)
                                ?: draft.getOrNull(col)?.takeIf { guess == null },
                            mark = guess?.marks?.getOrNull(col),
                            animate = animate,
                            delayMillis = col * REVEAL_STAGGER_MS,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One arch tile. A guess flips it over like a card: edge-on, the mark
 * lands, edge-back — while the letter stays upright the whole time.
 */
@Composable
private fun RevealTile(letter: Char?, mark: LetterMark?, animate: Boolean, delayMillis: Long) {
    var revealed by remember { mutableStateOf(!animate) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(animate) {
        if (!animate) {
            revealed = true
        } else {
            delay(delayMillis)
            rotation.animateTo(90f, tween(FLIP_HALF_MS))
            revealed = true
            rotation.animateTo(0f, tween(FLIP_HALF_MS))
        }
    }

    val scheme = MaterialTheme.colorScheme
    val (background, content, border) = when {
        !revealed -> Triple(scheme.surface, MaterialTheme.inkColor, scheme.outline)
        mark == LetterMark.HIT -> Triple(scheme.primary, scheme.onPrimary, Color.Transparent)
        mark == LetterMark.CLOSE -> Triple(scheme.surface, MaterialTheme.emberColor, MaterialTheme.emberColor)
        mark == LetterMark.MISS -> Triple(scheme.surfaceVariant, MaterialTheme.hintColor, Color.Transparent)
        else -> Triple(scheme.surface, MaterialTheme.inkColor, scheme.outline)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TILE_SIZE)
            .graphicsLayer {
                rotationX = rotation.value
                cameraDistance = 12f * density
            }
            .clip(archShape)
            .background(background)
            .then(
                if (border != Color.Transparent) {
                    Modifier.border(1.dp, border, archShape)
                } else {
                    Modifier
                },
            ),
    ) {
        if (letter != null) {
            Text(letter.uppercaseChar().toString(), style = MaterialTheme.readingDisplay, color = content)
        }
    }
}

/** The quiet record: a one-line tally and a bar per guess count. */
@Composable
private fun StatsFooter(played: Int, wins: Int, distribution: List<Int>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            "Played $played · won $wins",
            style = MaterialTheme.reference,
            color = MaterialTheme.hintColor,
        )
        val tallest = (distribution.maxOrNull() ?: 0).coerceAtLeast(1)
        distribution.forEachIndexed { slot, count ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.height(14.dp),
            ) {
                Text("${slot + 1}", style = MaterialTheme.label, color = MaterialTheme.hintColor)
                Box(
                    Modifier
                        .width(140.dp)
                        .height(6.dp)
                        .clip(archShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (count > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(count.toFloat() / tallest)
                                .height(6.dp)
                                .background(MaterialTheme.emberColor),
                        )
                    }
                }
                Text("$count", style = MaterialTheme.label, color = MaterialTheme.hintColor)
            }
        }
    }
}