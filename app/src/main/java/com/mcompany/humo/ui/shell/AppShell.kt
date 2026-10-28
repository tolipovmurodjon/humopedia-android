package com.mcompany.humo.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mcompany.humo.data.course.Level
import com.mcompany.humo.ui.glass.AuroraBackground
import com.mcompany.humo.ui.glass.Glass
import com.mcompany.humo.ui.glass.GlassStage
import com.mcompany.humo.ui.glass.LiquidBottomTab
import com.mcompany.humo.ui.glass.LiquidBottomTabs
import com.mcompany.humo.ui.glass.edgeFade
import com.mcompany.humo.ui.path.PathRoute as PathScreenRoute

private val BarTall = 64.dp
private val BarCompact = 48.dp
private val BarCompactWidth = 440.dp

/**
 * The native shell (PAGEL's AppShell, cut to Humopedia's five tabs): the aurora page is captured as the backdrop, the
 * liquid glass bar floats over it and bends what scrolls underneath. Tabs never slide (they are peers); a SegmentTick on
 * each change; Back leaves a lesson, then returns to Home, then leaves the app.
 */
@Composable
fun AppShell(
    startTab: Int = 0,
    /** Home's path; tests pass a fixed state instead of the Hilt ViewModel. */
    path: @Composable (PaddingValues, (Level) -> Unit) -> Unit = { padding, open -> PathScreenRoute(padding, onOpenLevel = open) },
) {
    val backdrop = rememberLayerBackdrop()
    var selected by rememberSaveable { mutableStateOf(startTab) }
    val stacks = Tab.entries.map { rememberNavBackStack(it.root) }
    val stack = stacks[selected]
    val onTop = stack.size == 1

    val compact = LocalConfiguration.current.screenHeightDp < 480
    val barHeight = if (compact) BarCompact else BarTall
    val barSpace = barHeight + 16.dp

    val haptics = LocalHapticFeedback.current
    var firstTab by remember { mutableStateOf(true) }
    LaunchedEffect(selected) { if (firstTab) firstTab = false else haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }

    BackHandler(enabled = selected != 0 && onTop) { selected = 0 }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val padding = PaddingValues(top = top, bottom = nav + if (onTop) barSpace else 16.dp)
            Box(
                Modifier.fillMaxSize()
                    // No full-screen offscreen layer in a cold start's first frame (GlassStage).
                    .then(if (!GlassStage.on) Modifier else Modifier.edgeFade(top + 14.dp, if (onTop) nav + barSpace + 8.dp else null)),
            ) {
                key(selected) {
                    NavDisplay(
                        backStack = stack,
                        onBack = { stack.removeLastOrNull() },
                        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>(), rememberViewModelStoreNavEntryDecorator<NavKey>()),
                        entryProvider = entryProvider<NavKey> {
                            entry<PathRoute> { path(padding) { stack.add(LessonRoute(it.n)) } }
                            entry<LessonRoute> { r -> LessonScreen(r.level, padding, onBack = { stack.removeLastOrNull() }) }
                            entry<WorldsRoute> { ComingScreen(Tab.Worlds, padding) }
                            entry<BattleRoute> { ComingScreen(Tab.Battle, padding) }
                            entry<LeaderboardRoute> { ComingScreen(Tab.Leaderboard, padding) }
                            entry<MeRoute> { ComingScreen(Tab.Me, padding) }
                        },
                    )
                }
            }
        }
        if (onTop) {
            LiquidBottomTabs(
                selectedTabIndex = { selected },
                onTabSelected = { selected = it },
                backdrop = backdrop,
                tabsCount = Tab.entries.size,
                barHeight = barHeight,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .then(if (compact) Modifier.widthIn(max = BarCompactWidth) else Modifier)
                    .fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            ) {
                Tab.entries.forEachIndexed { i, tab ->
                    val isSelected = selected == i
                    LiquidBottomTab(
                        onClick = { if (selected != i) selected = i },
                        modifier = Modifier.semantics { contentDescription = tab.label; this.selected = isSelected },
                    ) {
                        // Icons only (Boss 2026-10-28). Ink at 72 % on the glass; the selected one is filled and mint
                        // #1C7E69 itself, so it reads mint even where the lens layer is off (old phones, first frame).
                        Icon(
                            if (isSelected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) Glass.Primary else Glass.Ink.copy(alpha = 0.72f),
                            modifier = Modifier.size(if (compact) 24.dp else 26.dp),
                        )
                    }
                }
            }
        }
    }
    GlassStage.Advance(ready = true)
}
