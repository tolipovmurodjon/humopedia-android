package com.mcompany.humo.voice

import com.mcompany.humo.ui.lesson.TutorIntent
import com.mcompany.humo.ui.lesson.TutorIntent.*
import org.junit.Assert.assertEquals
import org.junit.Test

/** What a child says to Humo (typed or spoken) lands on one of the tutor's answers; the rest is "Say it another way?". */
class TutorIntentTest {
    private fun check(expected: TutorIntent, vararg phrases: String) {
        for (p in phrases) assertEquals("\"$p\"", expected, TutorIntent.match(p))
    }

    @Test fun `hints`() = check(Hint, "give me a hint", "can I get a hint", "I need help", "I'm stuck", "I don't know", "I dont get it", "I'm lost", "I am confused", "how do I do this", "any clue", "hnt please")

    @Test fun `why it works`() = check(Why, "why does it work", "why is this the answer", "can you explain", "explain it to me", "what is the reason", "how come")

    @Test fun `read the task`() = check(Read, "read the task", "read it to me", "what is the question", "what does it say", "say the instructions", "red the task")

    @Test fun `show me`() = check(ShowMe, "show me", "show me how", "just tell me the answer", "can you solve it", "demonstrate it")

    @Test fun `I see it`() = check(SeeIt, "I see it", "I get it", "got it", "I understand", "that makes sense")

    @Test fun `what went wrong`() = check(Look, "why was my answer wrong", "what went wrong", "what is my mistake", "look together")

    @Test fun `say it again`() = check(Repeat, "repeat that", "say that again", "pardon", "once more please", "what did you say", "I didn't hear")

    @Test fun `yes and no`() {
        check(Yes, "yes", "yeah", "okay", "sure")
        check(No, "no", "nope", "not really")
    }

    @Test fun `not understanding is a hint, understanding is not`() {
        assertEquals(Hint, TutorIntent.match("I don't understand"))
        assertEquals(SeeIt, TutorIntent.match("I understand"))
    }

    @Test fun `something else is unknown`() = check(Unknown, "", "   ", "banana", "what is your favourite colour", "jets are fast")
}
