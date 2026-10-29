package com.mcompany.humo.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mcompany.humo.ui.glass.Glass
import com.mcompany.humo.ui.glass.Pane

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
