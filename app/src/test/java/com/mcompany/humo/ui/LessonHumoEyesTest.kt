package com.mcompany.humo.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mcompany.humo.ui.lesson.Humo
import com.mcompany.humo.ui.lesson.HumoFace
import com.mcompany.humo.ui.lesson.HumoState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Boss's phone showed Humo with white eyes and no pupils. In every face, in each eye's box there must be dark pixels (a pupil, a closed
 * lid line or a happy arch): an eye that is only white fails. Sampled at 390 and 360 wide.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonHumoEyesTest {
    @get:Rule val compose = createComposeRule()

    private fun check() {
        compose.mainClock.autoAdvance = false
        val humo = HumoState(alive = true)
        compose.setContent { Humo(humo, Modifier.size(76.dp).testTag("humo")) }
        for (face in HumoFace.entries) {
            humo.face = face
            compose.mainClock.advanceTimeBy(1200)
            val img = compose.onNodeWithTag("humo").captureToImage()
            val w = img.width; val h = img.height
            val px = IntArray(w * h); img.readPixels(px, 0, 0, w, h)
            fun dark(x0: Int, x1: Int): Int {
                var n = 0
                for (y in (h * 0.42f).toInt()..(h * 0.78f).toInt()) for (x in x0 until x1) {
                    val c = px[y * w + x]; val l = ((c shr 16 and 255) * 3 + (c shr 8 and 255) * 6 + (c and 255)) / 10
                    if (c ushr 24 > 200 && l < 70) n++
                }
                return n
            }
            val left = dark((w * 0.18f).toInt(), (w * 0.5f).toInt()); val right = dark((w * 0.5f).toInt(), (w * 0.82f).toInt())
            assertTrue("$face: pupils missing (dark pixels left $left, right $right)", left >= 3 && right >= 3)
        }
    }

    @Test fun pupilsAt390() = check()
    @Config(qualifiers = "w360dp-h800dp-xxhdpi") @Test fun pupilsAt360() = check()
}
