package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.browser.BrowserEffect
import com.example.browser.BrowserViewModel
import com.example.core.CaptureSafeRequestInterceptor
import com.example.core.CaptureTestPage
import com.example.core.PrismEngine
import com.example.ui.components.BrowserTopBar
import com.example.ui.components.PrismStartPage
import com.example.ui.components.ProtectionDiagnosticsSheet
import com.example.ui.components.TabSwitcherSheet
import com.example.ui.theme.PrismAlabaster
import com.example.ui.theme.PrismEmerald
import com.example.ui.theme.PrismSurfaceElevated
import com.example.ui.theme.PrismTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BrowserActivity : AppCompatActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    private lateinit var topChromeCompose: ComposeView
    private lateinit var webViewContainer: FrameLayout
    private lateinit var overlayCompose: ComposeView

    private val tabWebViews = LinkedHashMap<Long, WebView>()
    private var activeTabId: Long = -1L
    private var statusBannerMessage by mutableStateOf<String?>(null)

    private val requestInterceptor by lazy {
        CaptureSafeRequestInterceptor(
            onDiagnosticsUpdated = {
                runOnUiThread {
                    viewModel.refreshDiagnostics(flagSecureCleared = isFlagSecureCleared())
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Layer 1: Explicitly clear FLAG_SECURE so screenshots, screen recording,
        // and live screen sharing (e.g. Gemini Live) are never blocked.
        ensureCaptureAllowedWindowFlags()

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        setContentView(R.layout.activity_browser)

        topChromeCompose = findViewById(R.id.top_chrome_compose)
        webViewContainer = findViewById(R.id.webview_container)
        overlayCompose = findViewById(R.id.overlay_compose)

        ViewCompat.setOnApplyWindowInsetsListener(webViewContainer) { view, insets ->
            val systemAndIme = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime()
            )
            view.updatePadding(bottom = systemAndIme.bottom)
            insets
        }

        viewModel.updateRuntimeCapabilities(
            flagSecureCleared = isFlagSecureCleared(),
            documentStartScriptSupported = WebViewFeature.isFeatureSupported(
                WebViewFeature.DOCUMENT_START_SCRIPT
            )
        )

        setupComposeChrome()
        observeBrowserEffects()
        registerBackHandler()

        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Layer 1: Re-assert FLAG_SECURE is cleared on every resume.
        ensureCaptureAllowedWindowFlags()
        activeWebView()?.onResume()
        viewModel.refreshDiagnostics(flagSecureCleared = isFlagSecureCleared())
    }

    override fun onPause() {
        activeWebView()?.onPause()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onDestroy() {
        tabWebViews.values.forEach { destroyWebViewSafely(it) }
        tabWebViews.clear()
        super.onDestroy()
    }

    private fun ensureCaptureAllowedWindowFlags() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun isFlagSecureCleared(): Boolean {
        return (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) == 0
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (!url.isNullOrBlank()) {
                viewModel.submitAddress(url)
            }
        }
    }

    private fun setupComposeChrome() {
        topChromeCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        topChromeCompose.setContent {
            PrismTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                BrowserTopBar(
                    uiState = uiState,
                    onAddressChanged = viewModel::onAddressChanged,
                    onAddressSubmit = { viewModel.submitAddress() },
                    onBackClick = viewModel::onBackClicked,
                    onForwardClick = viewModel::onForwardClicked,
                    onReloadOrStopClick = viewModel::onReloadOrStopClicked,
                    onHomeClick = viewModel::openHomeStartPage,
                    onTogglePrivateMode = viewModel::toggleNormalPrivateMode,
                    onOpenTabsClick = { viewModel.setTabSheetOpen(true) },
                    onOpenDiagnosticsClick = {
                        ensureCaptureAllowedWindowFlags()
                        viewModel.setDiagnosticsSheetOpen(
                            open = true,
                            flagSecureCleared = isFlagSecureCleared()
                        )
                    }
                )
            }
        }

        overlayCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        overlayCompose.setContent {
            PrismTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val banner = statusBannerMessage

                LaunchedEffect(banner) {
                    if (banner != null) {
                        delay(2600L)
                        statusBannerMessage = null
                    }
                }

                val shouldShowOverlay = uiState.showStartPage ||
                    uiState.isTabSheetOpen ||
                    uiState.isDiagnosticsSheetOpen ||
                    banner != null

                LaunchedEffect(shouldShowOverlay) {
                    overlayCompose.visibility = if (shouldShowOverlay) View.VISIBLE else View.GONE
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = uiState.showStartPage,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        PrismStartPage(
                            uiState = uiState,
                            onQuickLinkClick = { link -> viewModel.submitAddress(link.url) },
                            onOpenDiagnosticsClick = {
                                ensureCaptureAllowedWindowFlags()
                                viewModel.setDiagnosticsSheetOpen(
                                    open = true,
                                    flagSecureCleared = isFlagSecureCleared()
                                )
                            },
                            onNewPrivateTabClick = { viewModel.createNewTab(isPrivate = true) },
                            onOpenTabSheetClick = { viewModel.setTabSheetOpen(true) }
                        )
                    }

                    if (uiState.isTabSheetOpen) {
                        TabSwitcherSheet(
                            tabs = uiState.tabs,
                            onDismiss = { viewModel.setTabSheetOpen(false) },
                            onSelectTab = viewModel::selectTab,
                            onCloseTab = viewModel::closeTab,
                            onNewNormalTab = { viewModel.createNewTab(isPrivate = false) },
                            onNewPrivateTab = { viewModel.createNewTab(isPrivate = true) },
                            onWipeAllPrivateTabs = viewModel::closeAllPrivateTabs
                        )
                    }

                    if (uiState.isDiagnosticsSheetOpen) {
                        ProtectionDiagnosticsSheet(
                            uiState = uiState,
                            onDismiss = { viewModel.setDiagnosticsSheetOpen(false) },
                            onRunLiveCaptureTest = viewModel::runLiveCaptureSelfTest
                        )
                    }

                    AnimatedVisibility(
                        visible = banner != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        banner?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrismSurfaceElevated,
                                border = BorderStroke(1.dp, PrismEmerald.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrismAlabaster,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeBrowserEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is BrowserEffect.SwitchWebView -> {
                            attachTabWebView(effect.tabId, effect.isPrivate)
                        }

                        is BrowserEffect.LoadUrl -> {
                            val tab = viewModel.uiState.value.tabs.firstOrNull { it.id == effect.tabId }
                            val webView = getOrCreateWebView(
                                tabId = effect.tabId,
                                isPrivate = tab?.isPrivate ?: false
                            )
                            attachTabWebView(effect.tabId, tab?.isPrivate ?: false)
                            webView.loadUrl(effect.url)
                        }

                        is BrowserEffect.LoadCaptureTest -> {
                            val tab = viewModel.uiState.value.tabs.firstOrNull { it.id == effect.tabId }
                            val webView = getOrCreateWebView(
                                tabId = effect.tabId,
                                isPrivate = tab?.isPrivate ?: false
                            )
                            attachTabWebView(effect.tabId, tab?.isPrivate ?: false)
                            val html = CaptureTestPage.buildHtml(
                                flagSecureCleared = isFlagSecureCleared(),
                                nativeRustLoaded = PrismEngine.isNativeRustActive
                            )
                            webView.loadDataWithBaseURL(
                                CaptureTestPage.INTERNAL_URL,
                                html,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }

                        is BrowserEffect.DestroyTabWebView -> {
                            destroyTabWebView(effect.tabId, effect.wipePrivateData)
                        }

                        is BrowserEffect.DestroyPrivateWebViews -> {
                            effect.tabIds.forEach { id -> destroyTabWebView(id, wipePrivateData = false) }
                            clearPrivateBrowsingStorage(null)
                            ensureCaptureAllowedWindowFlags()
                        }

                        BrowserEffect.GoBack -> {
                            val wv = activeWebView()
                            if (wv != null && wv.canGoBack()) {
                                wv.goBack()
                            } else {
                                viewModel.openHomeStartPage()
                            }
                        }

                        BrowserEffect.GoForward -> {
                            activeWebView()?.takeIf { it.canGoForward() }?.goForward()
                        }

                        BrowserEffect.Reload -> {
                            val currentUrl = viewModel.uiState.value.currentUrl
                            if (currentUrl == CaptureTestPage.INTERNAL_URL) {
                                viewModel.runLiveCaptureSelfTest()
                            } else {
                                activeWebView()?.reload()
                            }
                        }

                        BrowserEffect.StopLoading -> {
                            activeWebView()?.stopLoading()
                        }

                        is BrowserEffect.ShowMessage -> {
                            statusBannerMessage = effect.message
                        }
                    }
                }
            }
        }
    }

    private fun registerBackHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val state = viewModel.uiState.value
                    when {
                        state.isDiagnosticsSheetOpen -> viewModel.setDiagnosticsSheetOpen(false)
                        state.isTabSheetOpen -> viewModel.setTabSheetOpen(false)
                        !state.showStartPage && activeWebView()?.canGoBack() == true -> {
                            activeWebView()?.goBack()
                        }
                        !state.showStartPage -> {
                            viewModel.openHomeStartPage()
                        }
                        else -> {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                            isEnabled = true
                        }
                    }
                }
            }
        )
    }

    private fun activeWebView(): WebView? = tabWebViews[activeTabId]

    private fun attachTabWebView(tabId: Long, isPrivate: Boolean) {
        val webView = getOrCreateWebView(tabId, isPrivate)
        activeTabId = tabId

        if (webViewContainer.childCount == 1 && webViewContainer.getChildAt(0) === webView) {
            viewModel.syncNavigationControls(webView.canGoBack(), webView.canGoForward())
            return
        }

        webViewContainer.removeAllViews()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webViewContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        viewModel.syncNavigationControls(webView.canGoBack(), webView.canGoForward())
        ensureCaptureAllowedWindowFlags()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun getOrCreateWebView(tabId: Long, isPrivate: Boolean): WebView {
        tabWebViews[tabId]?.let { return it }

        val webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#0F1216"))
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                databaseEnabled = !isPrivate
                @Suppress("DEPRECATION")
                saveFormData = !isPrivate
            }
        }

        // Layer 2A: Register Rust Document-Start JavaScript Shim before any page/iframe script executes.
        val shimJs = PrismEngine.getDocumentStartShim()
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, shimJs, setOf("*"))
        }

        val userAgent = webView.settings.userAgentString
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                // Layer 2B: Strip display-capture blocking Permissions-Policy/Feature-Policy headers
                // on main-frame HTML document requests.
                return requestInterceptor.interceptMainFrameHtml(
                    request = request,
                    userAgent = userAgent,
                    isPrivateTab = isPrivate
                )
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    view.evaluateJavascript(shimJs, null)
                }
                viewModel.onWebViewStateChanged(
                    tabId = tabId,
                    url = url.orEmpty(),
                    title = view.title.orEmpty(),
                    canGoBack = view.canGoBack(),
                    canGoForward = view.canGoForward(),
                    isLoading = true,
                    progress = view.progress.coerceAtLeast(10)
                )
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                viewModel.onWebViewStateChanged(
                    tabId = tabId,
                    url = url.orEmpty(),
                    title = view.title.orEmpty(),
                    canGoBack = view.canGoBack(),
                    canGoForward = view.canGoForward(),
                    isLoading = false,
                    progress = 100
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                viewModel.onWebViewStateChanged(
                    tabId = tabId,
                    url = view.url.orEmpty(),
                    title = view.title.orEmpty(),
                    canGoBack = view.canGoBack(),
                    canGoForward = view.canGoForward(),
                    isLoading = newProgress in 1..99,
                    progress = newProgress
                )
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                super.onReceivedTitle(view, title)
                viewModel.onWebViewStateChanged(
                    tabId = tabId,
                    url = view.url.orEmpty(),
                    title = title.orEmpty(),
                    canGoBack = view.canGoBack(),
                    canGoForward = view.canGoForward(),
                    isLoading = view.progress in 1..99,
                    progress = view.progress
                )
            }
        }

        tabWebViews[tabId] = webView
        return webView
    }

    private fun destroyTabWebView(tabId: Long, wipePrivateData: Boolean) {
        val webView = tabWebViews.remove(tabId)
        if (wipePrivateData) {
            clearPrivateBrowsingStorage(webView)
            // Keep FLAG_SECURE explicitly cleared even after private session cleanup.
            ensureCaptureAllowedWindowFlags()
        }
        if (webView != null) {
            destroyWebViewSafely(webView)
        }
    }

    private fun clearPrivateBrowsingStorage(targetWebView: WebView?) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        WebStorage.getInstance().deleteAllData()
        targetWebView?.apply {
            clearCache(true)
            clearHistory()
            clearFormData()
        }
    }

    private fun destroyWebViewSafely(webView: WebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.onPause()
        webView.removeAllViews()
        webView.destroy()
    }
}
