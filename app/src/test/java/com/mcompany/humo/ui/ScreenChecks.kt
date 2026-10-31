package com.mcompany.humo.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.TextLayoutResult
import java.io.File

/**
 * Looks at the text of the screen that is about to be photographed and writes what is wrong to
 * build/outputs/roborazzi/checks/problems.txt. The tests keep going so one run lists every problem; the CI step
 * .github/scripts/screen-checks.py then fails the build when the file is not empty (Boss, 2026-10-31: no cut text,
 * no emoji, no red).
 */
object ScreenChecks {
    private val emoji = Regex("""[\x{1F000}-\x{1FAFF}\x{2600}-\x{27BF}\x{2B00}-\x{2BFF}\x{FE0F}]""")
    private val problems = File("build/outputs/roborazzi/checks/problems.txt")

    fun check(rule: ComposeTestRule, screen: String) {
        try {
            val nodes = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true).fetchSemanticsNodes() +
                rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription), useUnmergedTree = true).fetchSemanticsNodes()
            for (node in nodes) {
                val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty()
                val described = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ").orEmpty()
                if (emoji.containsMatchIn(text) || emoji.containsMatchIn(described)) report("EMOJI", screen, "$text $described")
                val layout = mutableListOf<TextLayoutResult>()
                val read = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: continue
                if (!read(layout)) continue
                val result = layout.firstOrNull() ?: continue
                // hasVisualOverflow is true for every text under Robolectric (measured: "0", "Check"), so it proves nothing here.
                // Text is cut when letters were dropped (ellipsis, lines over maxLines) or its layout is bigger than its box.
                val ellipsized = (0 until result.lineCount).any { result.isLineEllipsized(it) }
                val droppedLines = result.multiParagraph.didExceedMaxLines
                val biggerThanBox = result.size.width > node.size.width + 1 || result.size.height > node.size.height + 1
                if (ellipsized || droppedLines || biggerThanBox) {
                    report(
                        "CUT-TEXT", screen,
                        "\"$text\" box ${node.size.width}x${node.size.height} px, layout ${result.size.width}x${result.size.height}, lines=${result.lineCount}, ellipsis=$ellipsized, droppedLines=$droppedLines",
                    )
                }
            }
        } catch (e: Throwable) {
            report("CHECK-ERROR", screen, e.toString())
        }
    }

    @Synchronized private fun report(kind: String, screen: String, detail: String) {
        problems.parentFile.mkdirs()
        problems.appendText("$kind | $screen | ${detail.replace('\n', ' ')}\n")
    }
}
