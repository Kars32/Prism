package com.example.browser

import android.net.Uri
import com.example.core.CaptureTestPage
import com.example.core.CoreDiagnostics
import com.example.core.TabMetadata

data class QuickLink(
    val title: String,
    val subtitle: String,
    val url: String,
    val isDiagnostic: Boolean = false
)

data class BrowserUiState(
    val tabs: List<TabMetadata> = emptyList(),
    val activeTabId: Long = -1L,
    val isActiveTabPrivate: Boolean = false,
    val addressInput: String = "",
    val currentUrl: String = "",
    val currentTitle: String = "New Tab",
    val isLoading: Boolean = false,
    val loadProgress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val showStartPage: Boolean = true,
    val isTabSheetOpen: Boolean = false,
    val isDiagnosticsSheetOpen: Boolean = false,
    val flagSecureCleared: Boolean = true,
    val documentStartScriptSupported: Boolean = true,
    val webContentsDebuggingEnabled: Boolean = false,
    val diagnostics: CoreDiagnostics = CoreDiagnostics(
        nativeRustActive = false,
        interceptedDocs = 0L,
        strippedHeaders = 0L,
        normalTabCount = 1,
        privateTabCount = 0,
        recentEvents = emptyList()
    ),
    val quickLinks: List<QuickLink> = defaultQuickLinks
)

sealed interface BrowserEffect {
    data class LoadUrl(val tabId: Long, val url: String) : BrowserEffect
    data class LoadCaptureTest(val tabId: Long) : BrowserEffect
    data class SwitchWebView(val tabId: Long, val isPrivate: Boolean) : BrowserEffect
    data class DestroyTabWebView(val tabId: Long, val wipePrivateData: Boolean) : BrowserEffect
    data class DestroyPrivateWebViews(val tabIds: List<Long>) : BrowserEffect
    data object GoBack : BrowserEffect
    data object GoForward : BrowserEffect
    data object Reload : BrowserEffect
    data object StopLoading : BrowserEffect
    data class ShowMessage(val message: String) : BrowserEffect
}

val defaultQuickLinks = listOf(
    QuickLink(
        title = "Capture Self-Test",
        subtitle = "Verify Layer 1 & 2 live",
        url = CaptureTestPage.INTERNAL_URL,
        isDiagnostic = true
    ),
    QuickLink(
        title = "DuckDuckGo",
        subtitle = "duckduckgo.com",
        url = "https://duckduckgo.com"
    ),
    QuickLink(
        title = "Wikipedia",
        subtitle = "wikipedia.org",
        url = "https://en.m.wikipedia.org"
    ),
    QuickLink(
        title = "GitHub",
        subtitle = "github.com",
        url = "https://github.com"
    ),
    QuickLink(
        title = "MDN Web Docs",
        subtitle = "developer.mozilla.org",
        url = "https://developer.mozilla.org"
    ),
    QuickLink(
        title = "Hacker News",
        subtitle = "news.ycombinator.com",
        url = "https://news.ycombinator.com"
    )
)

fun normalizeUrlOrSearch(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed == CaptureTestPage.INTERNAL_URL) return trimmed

    val lower = trimmed.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://")) {
        return trimmed
    }

    val looksLikeDomain = !trimmed.contains(" ") &&
        (trimmed.contains(".") || lower.startsWith("localhost"))

    return if (looksLikeDomain) {
        if (lower.startsWith("localhost")) "http://$trimmed" else "https://$trimmed"
    } else {
        "https://duckduckgo.com/?q=${Uri.encode(trimmed)}"
    }
}
