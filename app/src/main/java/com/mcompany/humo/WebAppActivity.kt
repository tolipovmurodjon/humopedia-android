package com.mcompany.humo

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.window.OnBackInvokedDispatcher

/**
 * The web app in our own WebView instead of Chrome's Trusted Web Activity. In the TWA, Chrome 154 owns the window and
 * asks Android for white navigation-bar icons over our white bar, and ignores NAVIGATION_BAR_COLOR (A05s, 2026-10-03).
 * Here this activity owns the bars: dark icons, and a status bar painted with the page's theme-color.
 */
class WebAppActivity : android.app.Activity() {

    private lateinit var webView: WebView
    private lateinit var statusBar: View
    private lateinit var cover: HumoCover
    // The address a main-frame load failed on, kept until it loads; null while the page is fine.
    private var failedUrl: String? = null
    private var network: ConnectivityManager.NetworkCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)

        statusBar = View(this).apply { setBackgroundColor(Color.WHITE) }
        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Humo speaks when a lesson opens, before the child has touched anything.
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = "${settings.userAgentString} HumopediaApp/${packageManager.getPackageInfo(packageName, 0).versionName}"
            webViewClient = ShellClient()
            addJavascriptInterface(Bridge(), "HumoBridge")
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        cover = HumoCover(this) { retry() }
        val page = FrameLayout(this).apply {
            addView(webView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(cover, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(statusBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0))
            addView(page, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)
        fitBetweenSystemBars(root)
        listenForBack()
        retryWhenOnline()

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl((intent.data ?: Uri.parse(getString(R.string.launch_url))).toString())
        }
    }

    /**
     * Android 15+ draws every app edge to edge, so the page would sit under both bars. The page gets the space between
     * them (and above the keyboard); the strip under the status bar takes the page's top colour.
     */
    private fun fitBetweenSystemBars(root: LinearLayout) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        window.setDecorFitsSystemWindows(false)
        window.isNavigationBarContrastEnforced = false
        root.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsets.Type.ime())
            statusBar.layoutParams = statusBar.layoutParams.apply { height = bars.top }
            root.setPadding(bars.left, 0, bars.right, maxOf(bars.bottom, keyboard.bottom))
            WindowInsets.CONSUMED
        }
    }

    private fun paintStatusBar(colour: Int) {
        val light = Color.luminance(colour) > 0.5f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            statusBar.setBackgroundColor(colour)
            window.insetsController?.setSystemBarsAppearance(
                if (light) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            )
        } else {
            @Suppress("DEPRECATION")
            window.statusBarColor = colour
        }
    }

    // Android 16 no longer calls onBackPressed for apps that target it; the dispatcher is the only way to hear back.
    private fun listenForBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { goBack() }
        }
    }

    @Deprecated("Android 12 and lower only; newer versions use the back dispatcher.")
    override fun onBackPressed() = goBack()

    // The web app keeps its own history (the keypad, sheets and menus close on back), so back walks the page history.
    // The page answers first: WebView skips history entries a page added without a tap, so the lesson's own entries
    // (keypad, pencil, the leave sheet) are invisible to canGoBack and back closed the app (A05s, 2026-10-03). The page
    // steps through them itself with history.back(), which is never skipped.
    private fun goBack() {
        if (failedUrl != null) return walkBack()
        webView.evaluateJavascript("window.humoBack ? window.humoBack() : false") { handled ->
            if (handled != "true") walkBack()
        }
    }

    private fun walkBack() {
        if (!webView.canGoBack()) return finish()
        // Back from the no-internet screen loads the page before; Humo stays on until it has drawn or fails too.
        if (failedUrl != null) { failedUrl = null; cover.showLoading() }
        webView.goBack()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { webView.loadUrl(it.toString()) }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    private fun retry(quiet: Boolean = false) {
        val url = failedUrl ?: return
        failedUrl = null
        if (!quiet) cover.showLoading()
        webView.loadUrl(url)
    }

    // A school Wi-Fi's DNS dropped our name for minutes while google.com resolved (A05s, 2026-10-03): the network never
    // goes away, so no callback fires. The screen tries again by itself every few seconds, without flickering.
    private val quietRetry = Runnable { if (cover.isOffline) retry(quiet = true) }

    // Wi-Fi or data coming back loads the page again without a tap.
    private fun retryWhenOnline() {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        network = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(net: Network) { runOnUiThread { if (failedUrl != null && cover.isOffline) retry() } }
        }.also { manager.registerDefaultNetworkCallback(it) }
    }

    override fun onDestroy() {
        network?.let { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) }
        webView.removeCallbacks(quietRetry)
        webView.destroy()
        super.onDestroy()
    }

    private fun isOurSite(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return uri.scheme == "https" && (host == getString(R.string.web_host) || host == SITE || host.endsWith(".$SITE"))
    }

    // A page link may name an app (payment apps use intent: links). It may only open what a browser link could open:
    // no direct component, and a fallback page only over https.
    private fun openOutside(uri: Uri) {
        val intent = if (uri.scheme == "intent") {
            try { Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME) } catch (_: java.net.URISyntaxException) { return }
                .apply { component = null; selector = null }
        } else Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent.addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            val fallback = intent.getStringExtra("browser_fallback_url")?.let(Uri::parse) ?: return
            if (fallback.scheme == "https") openOutside(fallback)
        }
    }

    private inner class ShellClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame || isOurSite(request.url)) return false
            openOutside(request.url)
            return true
        }

        // The page sets theme-color to whatever is drawn at its top (StatusBarColor.tsx); Chrome read it for the TWA,
        // here a watcher hands every change to the bridge.
        override fun onPageFinished(view: WebView, url: String) {
            view.evaluateJavascript(WATCH_THEME_COLOR, null)
            pageShown()
        }

        // Humo covers the page until it has drawn, so a slow first load (an empty cache, a weak signal) is never a
        // white screen, and a failed one never shows Android's "Webpage not available" (A05s, 2026-10-03).
        override fun onPageCommitVisible(view: WebView, url: String) = pageShown()

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (!request.isForMainFrame) return
            failedUrl = request.url.toString()
            cover.showOffline()
            view.removeCallbacks(quietRetry)
            view.postDelayed(quietRetry, QUIET_RETRY_MS)
        }

        // A crashed or killed renderer would leave a blank white screen; start the screen again instead.
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            recreate()
            return true
        }
    }

    private fun pageShown() {
        if (failedUrl == null) cover.hide()
    }

    private inner class Bridge {
        @JavascriptInterface
        fun statusBar(css: String) {
            val colour = parseCssColour(css) ?: return
            runOnUiThread { paintStatusBar(colour) }
        }
    }

    private companion object {
        const val SITE = "humopedia.com"
        const val QUIET_RETRY_MS = 5_000L
        const val WATCH_THEME_COLOR = """(function () {
            if (window.__humoBar) return; window.__humoBar = true;
            var send = function () {
              var m = document.querySelector('meta[name="theme-color"]');
              if (m && window.HumoBridge) HumoBridge.statusBar(m.content);
            };
            new MutationObserver(send).observe(document.head, { subtree: true, childList: true, attributes: true, attributeFilter: ['content'] });
            send();
        })();"""

        fun parseCssColour(css: String): Int? {
            val rgb = Regex("""rgba?\(\s*(\d+)[,\s]+(\d+)[,\s]+(\d+)""").find(css)
            if (rgb != null) {
                val (r, g, b) = rgb.destructured
                return Color.rgb(r.toInt(), g.toInt(), b.toInt())
            }
            return try { Color.parseColor(css.trim()) } catch (_: IllegalArgumentException) { null }
        }
    }
}
