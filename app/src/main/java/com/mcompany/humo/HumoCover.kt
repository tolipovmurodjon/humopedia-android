package com.mcompany.humo

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * What the WebView shows instead of white or Android's error page: Humo alone while the first page loads, and Humo
 * with one line and the mint button when there is no internet. Drawn natively, so it works with an empty cache and no
 * network at all.
 */
class HumoCover(context: Context, onRetry: () -> Unit) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    private val title = text(22f, Color.parseColor("#1E293B"), bold = true).apply { setText(R.string.offline_title) }
    private val line = text(16f, Color.parseColor("#64748B"), bold = false).apply { setText(R.string.offline_line) }
    private val button = TextView(context).apply {
        setText(R.string.offline_retry)
        setTextColor(Color.WHITE)
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.04f
        gravity = Gravity.CENTER
        isAllCaps = true
        background = ledge(pressed = false)
        setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { v.background = ledge(pressed = true); v.translationY = dp(3).toFloat() }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.background = ledge(pressed = false); v.translationY = 0f }
            }
            false
        }
        setOnClickListener { onRetry() }
    }
    var isOffline = false
        private set

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.WHITE)
        isClickable = true
        // Android's splash centres Humo on the whole screen, this cover between the bars: 20 dp on top puts him on the
        // same pixel, at the splash's 192 dp, so the hand-over does not jump (measured from a recording, A05s).
        setPadding(dp(24), dp(20), dp(24), 0)
        addView(ImageView(context).apply { setImageResource(R.drawable.splash); contentDescription = null }, LayoutParams(dp(192), dp(192)))
        addView(title, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })
        addView(line, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        addView(button, LayoutParams(LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(32) })
        showLoading()
    }

    fun showLoading() = show(offline = false)

    fun showOffline() = show(offline = true)

    fun hide() {
        isOffline = false
        visibility = GONE
    }

    private fun show(offline: Boolean) {
        isOffline = offline
        // While loading Humo sits alone in the middle, where the splash left him.
        val words = if (offline) VISIBLE else GONE
        title.visibility = words
        line.visibility = words
        button.visibility = words
        visibility = VISIBLE
    }

    private fun text(size: Float, colour: Int, bold: Boolean) = TextView(context).apply {
        textSize = size
        setTextColor(colour)
        gravity = Gravity.CENTER
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    // The web app's mint button: Humo mint on a darker ledge that the press pushes into.
    private fun ledge(pressed: Boolean): LayerDrawable {
        val radius = dp(16).toFloat()
        val base = GradientDrawable().apply { cornerRadius = radius; setColor(Color.parseColor("#23A06C")) }
        val face = GradientDrawable().apply { cornerRadius = radius; setColor(Color.parseColor("#3DC98C")) }
        return LayerDrawable(arrayOf(base, face)).apply { setLayerInset(1, 0, 0, 0, if (pressed) dp(1) else dp(4)) }
    }
}
