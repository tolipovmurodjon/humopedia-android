package com.mcompany.humo.ui.path

import kotlin.math.sin

enum class StoneState { Done, Current, Locked }

fun stoneState(level: Int, current: Int): StoneState = when {
    level < current -> StoneState.Done
    level == current -> StoneState.Current
    else -> StoneState.Locked
}

/**
 * Where each stone sits on the river, in dp, inside a content box [height] tall and [width] wide. Level 1 is at the
 * bottom and the river climbs; it swings left and right on a slow sine so the eye follows it up. Pure numbers, so the
 * layout is tested on the JVM and the screen only reads it.
 */
class PathLayout(val count: Int, val width: Float) {
    val gap = 132f
    val top = 260f
    val bottom = 200f
    val height = top + (count - 1) * gap + bottom
    private val swing = (width * 0.24f).coerceAtMost(120f)

    fun x(i: Int): Float = width / 2f + swing * sin(i * 0.95f + 0.6f)

    /** [i] is 0-based: 0 = level 1, at the bottom. */
    fun y(i: Int): Float = height - bottom - i * gap

    /**
     * The scroll offset that puts stone [i] at thumb height: 62 % down a [viewport]-tall screen, the easy reach of a
     * one-handed grip (NATIVE-BRIEF N0: "the current stone at thumb height").
     */
    fun scrollFor(i: Int, viewport: Float): Float = (y(i) - viewport * THUMB).coerceIn(0f, (height - viewport).coerceAtLeast(0f))

    companion object {
        const val THUMB = 0.62f
    }
}
