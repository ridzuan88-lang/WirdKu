package com.wirdku.app

import com.wirdku.app.data.ArabicText
import org.junit.Assert.*
import org.junit.Test

class ArabicTextTest {
    private val bismillah = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"

    @Test fun segmentsOnWhitespaceOnly() {
        val w = ArabicText.segment(bismillah)
        assertEquals(4, w.size)
        assertEquals("بِسْمِ", w[0])
    }

    @Test fun marksNeverDetach() {
        ArabicText.segment(bismillah).forEach { word ->
            assertFalse(ArabicText.isCombiningMark(word.first()))
        }
    }

    @Test fun roundTripPreservesText() {
        assertEquals(bismillah, ArabicText.segment(bismillah).joinToString(" "))
    }

    @Test fun strayMarkMergesIntoPreviousWord() {
        val w = ArabicText.segment("الرَّحْمَٰنِ \u0651")
        assertEquals(1, w.size)
    }

    @Test fun baseLettersStripDiacritics() {
        val l = ArabicText.baseLetters("بِسْمِ")
        assertEquals(listOf('ب', 'س', 'م'), l)
    }

    @Test fun detectsShaddahAndDaggerAlif() {
        val m = ArabicText.marksIn("الرَّحْمَٰنِ")
        assertTrue('\u0651' in m)
        assertTrue('\u0670' in m)
    }
}
