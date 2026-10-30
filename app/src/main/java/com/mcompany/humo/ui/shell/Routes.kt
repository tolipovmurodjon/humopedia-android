package com.mcompany.humo.ui.shell

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.mcompany.humo.ui.glass.TabIcons
import kotlinx.serialization.Serializable

/** Type-safe Navigation 3 routes. Each tab has its own back stack; a lesson opens on top of Home's. */
@Serializable data object PathRoute : NavKey
@Serializable data object WorldsRoute : NavKey
@Serializable data object BattleRoute : NavKey
@Serializable data object LeaderboardRoute : NavKey
@Serializable data object MeRoute : NavKey
/** Lesson [level] of the course (its place on the path, 1 = lesson 1.1), opened by one tap on its island. */
@Serializable data class LessonRoute(val level: Int) : NavKey

/**
 * The bar, icons only (Boss 2026-10-28, Instagram style). [label] is the Uzbek name TalkBack reads; it is never drawn.
 */
enum class Tab(val label: String, val root: NavKey, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Bosh sahifa", PathRoute, TabIcons.Home, TabIcons.HomeFilled),
    Worlds("Olamlar", WorldsRoute, TabIcons.Worlds, TabIcons.WorldsFilled),
    Battle("Jang", BattleRoute, TabIcons.Battle, TabIcons.BattleFilled),
    Leaderboard("Reyting", LeaderboardRoute, TabIcons.Leaderboard, TabIcons.LeaderboardFilled),
    Me("Men", MeRoute, TabIcons.Me, TabIcons.MeFilled),
}
