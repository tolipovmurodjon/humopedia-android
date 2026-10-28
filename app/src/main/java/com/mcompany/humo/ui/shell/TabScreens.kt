package com.mcompany.humo.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mcompany.humo.data.course.CoordinateTransformations
import com.mcompany.humo.data.course.firstLessonLabel
import com.mcompany.humo.ui.glass.Glass
import com.mcompany.humo.ui.glass.Pane
import com.mcompany.humo.ui.glass.TabIcons
import com.mcompany.humo.ui.glass.pressScale

/** What each tab will be, one line each (UI words are Uzbek and call the learner "siz"). */
val ComingLines = mapOf(
    Tab.Worlds to "Darslarda topgan qismlaringizdan oʻz olamingizni qurasiz.",
    Tab.Battle to "Tengdoshingiz bilan bir xil masalada jonli bellashasiz.",
    Tab.Leaderboard to "Har hafta ligada oʻrningizni koʻrasiz va yuqoriga chiqasiz.",
    Tab.Me to "Profilingiz va 6 ta esdalik sovgʻangiz turadigan javon.",
)

/** A tab that is not built yet: its name and one line on a glass pane, nothing pretending to work. */
@Composable
fun ComingScreen(tab: Tab, padding: PaddingValues) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(tab.label, style = MaterialTheme.typography.headlineLarge, color = Glass.Ink, modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() })
        Spacer(Modifier.height(16.dp))
        Pane(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("Tez orada", style = MaterialTheme.typography.labelSmall, color = Glass.PrimaryInk)
                Spacer(Modifier.height(4.dp))
                Text(ComingLines.getValue(tab), style = MaterialTheme.typography.bodyLarge, color = Glass.Ink)
            }
        }
    }
}

/**
 * The lesson's placeholder (the player comes next phase, ported from docs/courses/15-coordinate-transformations):
 * the course, the level's first lesson, and a way back. Lesson words are English.
 */
@Composable
fun LessonScreen(level: Int, padding: PaddingValues, onBack: () -> Unit) {
    val l = CoordinateTransformations.levels.first { it.n == level }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier.padding(top = 8.dp).size(48.dp).pressScale(interaction)
                .clickable(interaction, null, role = Role.Button, onClickLabel = "Orqaga", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Pane(Modifier.size(44.dp), radius = 22.dp) {
                Icon(TabIcons.ChevronLeft, contentDescription = "Orqaga", tint = Glass.Ink, modifier = Modifier.align(Alignment.Center).size(24.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(CoordinateTransformations.title, style = MaterialTheme.typography.labelSmall, color = Glass.Muted)
        Text("Level ${l.n} · ${l.title}", style = MaterialTheme.typography.headlineSmall, color = Glass.Ink, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(16.dp))
        Pane(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(l.firstLessonLabel(), style = MaterialTheme.typography.titleMedium, color = Glass.Ink)
                Spacer(Modifier.height(4.dp))
                Text("The lesson player arrives in the next build.", style = MaterialTheme.typography.bodyMedium, color = Glass.Muted)
            }
        }
    }
}
