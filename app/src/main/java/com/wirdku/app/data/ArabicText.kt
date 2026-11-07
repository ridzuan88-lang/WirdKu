package com.wirdku.app.data

/** Unicode-safe Arabic helpers. Words are split ONLY on whitespace, so combining marks never detach. */
object ArabicText {
    fun isCombiningMark(c: Char): Boolean =
        c in '\u064B'..'\u065F' || c == '\u0670' || c in '\u06D6'..'\u06ED' ||
            c in '\u0610'..'\u061A'

    fun hasArabicLetter(t: String) = t.any { it in '\u0621'..'\u064A' || it == '\u0671' }

    fun isTatweelOrMarkOnly(token: String) = token.isNotEmpty() && token.all { isCombiningMark(it) }

    /** Split into tokens; a token consisting only of marks (stray) is merged into the previous word. */
    fun segment(text: String): List<String> {
        val raw = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val out = ArrayList<String>()
        for (t in raw) {
            if (isTatweelOrMarkOnly(t) && out.isNotEmpty()) out[out.lastIndex] = out.last() + t
            else if (!hasArabicLetter(t) && out.isNotEmpty()) out[out.lastIndex] = out.last() + " " + t
            else out.add(t)
        }
        return out
    }

    /** Base letters of a word with all diacritics/marks stripped, in order, de-duplicated. */
    fun baseLetters(word: String): List<Char> =
        word.filter { it in '\u0621'..'\u064A' || it == '\u0671' || it == '\u0649' || it == '\u0629' }
            .map { normalize(it) }.distinct()

    private fun normalize(c: Char): Char = when (c) {
        '\u0622', '\u0623', '\u0625', '\u0671' -> 'ا'
        '\u0649' -> 'ي'
        '\u0629' -> 'ه'
        '\u0624' -> 'و'
        '\u0626' -> 'ي'
        else -> c
    }

    fun marksIn(word: String): List<Char> = word.filter { isCombiningMark(it) }.toList().distinct()
}
