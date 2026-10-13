package app.scripture

import app.scripture.domain.chunkVerse
import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkingTest {

    @Test
    fun sentencesAreTheNaturalChunks() {
        val chunks = chunkVerse("He will destroy death forever. The Lord will wipe away tears.")
        assertEquals(
            listOf("He will destroy death forever.", "The Lord will wipe away tears."),
            chunks,
        )
    }

    @Test
    fun semicolonsAndColonsBreakToo() {
        val chunks = chunkVerse("He has come to judge the peoples; he will settle the matter for many: the Lord God reigns.")
        assertEquals(
            listOf(
                "He has come to judge the peoples;",
                "he will settle the matter for many:",
                "the Lord God reigns.",
            ),
            chunks,
        )
    }

    @Test
    fun shortFragmentsRideWithTheSentenceThatFollows() {
        val chunks = chunkVerse("Rejoice! The Lord your God is at hand today.")
        assertEquals(
            listOf("Rejoice! The Lord your God is at hand today."),
            chunks,
        )
    }

    @Test
    fun aTrailingShortFragmentRidesWithTheSentenceBefore() {
        val chunks = chunkVerse("The Lord is my shepherd guiding me. He restores me.")
        assertEquals(
            listOf("The Lord is my shepherd guiding me. He restores me."),
            chunks,
        )
    }

    @Test
    fun longSentencesSplitAtCommas() {
        val chunks = chunkVerse(
            "He will destroy this shroud that is spread over all peoples, " +
                "the covering that is spread over all nations, " +
                "and he will swallow up death forever in victory.",
        )
        assertEquals(
            listOf(
                "He will destroy this shroud that is spread over all peoples,",
                "the covering that is spread over all nations,",
                "and he will swallow up death forever in victory.",
            ),
            chunks,
        )
    }

    @Test
    fun aLongSentenceWithoutCommasStaysWhole() {
        val chunks = chunkVerse("one two three four five six seven eight nine ten eleven twelve thirteen")
        assertEquals(1, chunks.size)
    }

    @Test
    fun emptyTextChunksToNothing() {
        assertEquals(emptyList(), chunkVerse("   "))
    }

    @Test
    fun isaiahTwentyFiveEightComesOutReadable() {
        // The example from the design discussion: a real verse with three or
        // four natural chunks, none too long to recite in a breath.
        val chunks = chunkVerse(
            "He will destroy this shroud, the shroud over all peoples, " +
                "the covering spread over all nations. " +
                "He will swallow up death forever, and the Lord God will wipe away " +
                "the tears from all faces.",
        )
        assertEquals(true, chunks.size in 3..4)
        assertEquals(true, chunks.all { it.split(" ").size in 4..12 })
    }
}