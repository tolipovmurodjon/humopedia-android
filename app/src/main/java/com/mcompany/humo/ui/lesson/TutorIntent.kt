package com.mcompany.humo.ui.lesson

/** What a child asked Humo, from words they typed or said. The tutor has a finite set of answers, so this only picks one. */
internal enum class TutorIntent { Hint, Why, Read, ShowMe, SeeIt, Look, Repeat, Yes, No, Unknown;

    companion object {
        private val negators = setOf("dont", "not", "cant", "cannot", "no", "never", "dunno")

        private fun tokens(text: String): List<String> =
            text.lowercase().replace("'", "").replace("’", "").replace(Regex("[^a-z0-9 ]"), " ").split(' ').filter { it.isNotEmpty() }

        /** Edit distance between two words, capped: speech recognisers and small fingers both slip by a letter. */
        private fun near(a: String, b: String): Boolean {
            if (a == b) return true
            val allowed = when { b.length >= 8 -> 2; b.length >= 4 -> 1; else -> 0 }
            if (allowed == 0 || kotlin.math.abs(a.length - b.length) > allowed) return false
            val d = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) d[i][0] = i
            for (j in 0..b.length) d[0][j] = j
            for (i in 1..a.length) for (j in 1..b.length) {
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            return d[a.length][b.length] <= allowed
        }

        fun match(text: String): TutorIntent {
            val t = tokens(text)
            if (t.isEmpty()) return Unknown
            val line = t.joinToString(" ")
            fun word(vararg ws: String) = ws.any { w -> t.any { near(it, w) } }
            fun phrase(vararg ps: String) = ps.any { it in line }
            val negated = t.any { it in negators }
            return when {
                word("read", "task", "question", "problem", "instructions") || phrase("what does it say", "what do i have to do") -> Read
                word("wrong", "mistake", "mistook", "incorrect") || phrase("went wrong", "look together", "look at it") -> Look
                word("why", "explain", "reason", "because") || phrase("how come", "does it work", "make sense") && !negated -> Why
                "show" in t || word("answer", "solve", "demonstrate") || phrase("tell me how", "do it for me") -> ShowMe
                word("hint", "help", "clue", "stuck", "lost", "confused", "tip", "idea") || phrase("dont know", "dont get", "dont understand", "not sure", "what do i do", "how do i") -> Hint
                phrase("i see", "i get it", "got it", "i understand", "makes sense", "i know") && !negated -> SeeIt
                word("repeat", "pardon", "again", "huh") || phrase("say that", "once more", "what did you say", "didnt hear", "come again") -> Repeat
                word("yes", "yeah", "yep", "sure", "ok", "okay", "yup") -> Yes
                word("no", "nope", "nah") || phrase("not really") -> No
                else -> Unknown
            }
        }
    }
}
