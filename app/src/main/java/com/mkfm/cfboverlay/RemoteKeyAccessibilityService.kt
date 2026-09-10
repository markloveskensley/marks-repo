package com.mkfm.cfboverlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.WebViewClient

/**
 * Global remote-button-triggered overlay for the NCAA football scoreboard.
 *
 * How this works, in short:
 * - Android's Accessibility API lets a service request FLAG_REQUEST_FILTER_KEY_EVENTS,
 *   which delivers raw key events to onKeyEvent() below REGARDLESS of which
 *   app currently has foreground focus. This is the same mechanism
 *   Projectivy Launcher itself uses for its own remote-shortcut menu.
 * - TYPE_ACCESSIBILITY_OVERLAY is a window type reserved for accessibility
 *   services specifically for drawing UI on top of everything else — no
 *   SYSTEM_ALERT_WINDOW permission/appops grant needed, unlike a normal app.
 * - The overlay content is just the existing self-hosted scoreboard page,
 *   loaded in a plain WebView. All the panel/scrolling/settings logic
 *   already built into that page keeps working unchanged.
 *
 * Setup required after installing:
 * 1. Settings > Accessibility > CFB Scoreboard Overlay > turn ON.
 *    (Or via adb: `adb shell settings put secure enabled_accessibility_services
 *    com.mkfm.cfboverlay/.RemoteKeyAccessibilityService`, followed by
 *    `adb shell settings put secure accessibility_enabled 1` — faster than
 *    navigating the TV's Accessibility settings menu with a remote.)
 * 2. Press the dedicated remote button from any app. First press shows the
 *    overlay, second press hides it. Nothing else needs to be running.
 */
class RemoteKeyAccessibilityService : AccessibilityService() {

    // ------------------------- Configuration -------------------------

    /** LAN URL of the self-hosted scoreboard page. */
    private val overlayUrl = "http://192.168.1.135:8097/cfb-scoreboard-overlay.html"

    /**
     * The dedicated remote button that toggles the overlay. Picked
     * specifically because it's not used by streaming apps, so it's safe
     * to consume unconditionally — no long-press/short-press logic needed.
     * Change this if you bind a different button (check the exact keycode
     * with Button Mapper's key-capture screen first).
     */
    private val toggleKeyCode = KeyEvent.KEYCODE_TV_SATELLITE_SERVICE

    /** Overlay width in pixels — matches roughly the panel size already tuned in the web page. */
    private val overlayWidthPx = 480

    // --------------------------------------------------------------

    private var windowManager: WindowManager? = null
    private var overlayView: WebView? = null
    private var isShowing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == toggleKeyCode) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                toggleOverlay()
            }
            // Consume both DOWN and UP — safe since nothing else uses this key.
            return true
        }
        return false
    }

    private fun toggleOverlay() {
        if (isShowing) hideOverlay() else showOverlay()
    }

    private fun showOverlay() {
        if (isShowing) return
        val wm = windowManager ?: return

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            setBackgroundColor(0) // transparent — matches the page's own transparent stage
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    // The page starts closed/off-screen by default (so it
                    // behaves correctly in a normal browser too). Tell it
                    // directly to open, since no keypress is being sent
                    // into the WebView itself for this integration.
                    view.evaluateJavascript(
                        "window.__setOverlayOpen && window.__setOverlayOpen(true);",
                        null
                    )
                }
            }
            loadUrl(overlayUrl)
        }

        val params = WindowManager.LayoutParams(
            overlayWidthPx,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0, // focusable + touchable, so D-pad scrolling reaches the WebView's JS
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.END or Gravity.TOP

        wm.addView(webView, params)
        webView.requestFocus()

        overlayView = webView
        isShowing = true
    }

    private fun hideOverlay() {
        val wm = windowManager ?: return
        overlayView?.let {
            wm.removeView(it)
            it.destroy()
        }
        overlayView = null
        isShowing = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Required override — this service doesn't act on accessibility
        // events themselves, only on the raw key events above.
    }

    override fun onInterrupt() {
        hideOverlay()
    }
}
