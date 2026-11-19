package com.example.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.core.CaptureTestPage
import com.example.core.PrismEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        BrowserUiState(
            webContentsDebuggingEnabled = BuildConfig.DEBUG,
            diagnostics = PrismEngine.getDiagnostics()
        )
    )
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BrowserEffect>(extraBufferCapacity = 32)
    val effects: SharedFlow<BrowserEffect> = _effects.asSharedFlow()

    init {
        ensureInitialTab()
    }

    private fun ensureInitialTab() {
        val existingTabs = PrismEngine.getTabs()
        if (existingTabs.isEmpty()) {
            val tabId = PrismEngine.createTab(
                isPrivate = false,
                initialUrl = "",
                initialTitle = "Start Page"
            )
            syncStateFromEngine(activeOverrideId = tabId)
            emitEffect(BrowserEffect.SwitchWebView(tabId = tabId, isPrivate = false))
        } else {
            syncStateFromEngine()
            val active = existingTabs.firstOrNull { it.isActive } ?: existingTabs.first()
            emitEffect(BrowserEffect.SwitchWebView(tabId = active.id, isPrivate = active.isPrivate))
        }
    }

    fun updateRuntimeCapabilities(
        flagSecureCleared: Boolean,
        documentStartScriptSupported: Boolean
    ) {
        _uiState.update { state ->
            state.copy(
                flagSecureCleared = flagSecureCleared,
                documentStartScriptSupported = documentStartScriptSupported,
                diagnostics = PrismEngine.getDiagnostics()
            )
        }
    }

    fun refreshDiagnostics(flagSecureCleared: Boolean? = null) {
        _uiState.update { state ->
            state.copy(
                flagSecureCleared = flagSecureCleared ?: state.flagSecureCleared,
                diagnostics = PrismEngine.getDiagnostics()
            )
        }
    }

    fun onAddressChanged(newInput: String) {
        _uiState.update { it.copy(addressInput = newInput) }
    }

    fun submitAddress(rawInput: String = _uiState.value.addressInput) {
        val normalized = normalizeUrlOrSearch(rawInput)
        if (normalized.isEmpty()) return

        val tabId = _uiState.value.activeTabId
        if (tabId <= 0L) return

        if (normalized == CaptureTestPage.INTERNAL_URL) {
            runLiveCaptureSelfTest()
            return
        }

        PrismEngine.updateTab(tabId, normalized, normalized)
        _uiState.update { state ->
            state.copy(
                addressInput = normalized,
                currentUrl = normalized,
                showStartPage = false,
                isLoading = true,
                loadProgress = 12,
                isTabSheetOpen = false,
                isDiagnosticsSheetOpen = false
            )
        }
        syncTabsOnly()
        emitEffect(BrowserEffect.LoadUrl(tabId = tabId, url = normalized))
    }

    fun runLiveCaptureSelfTest() {
        val tabId = _uiState.value.activeTabId
        if (tabId <= 0L) return

        PrismEngine.updateTab(tabId, CaptureTestPage.INTERNAL_URL, "Capture Self-Test")
        _uiState.update { state ->
            state.copy(
                addressInput = CaptureTestPage.INTERNAL_URL,
                currentUrl = CaptureTestPage.INTERNAL_URL,
                currentTitle = "Prism Live Capture Self-Test",
                showStartPage = false,
                isLoading = false,
                loadProgress = 100,
                isDiagnosticsSheetOpen = false,
                isTabSheetOpen = false
            )
        }
        syncTabsOnly()
        emitEffect(BrowserEffect.LoadCaptureTest(tabId = tabId))
    }

    fun openHomeStartPage() {
        _uiState.update { state ->
            state.copy(
                showStartPage = true,
                addressInput = "",
                isTabSheetOpen = false,
                isDiagnosticsSheetOpen = false
            )
        }
    }

    fun createNewTab(isPrivate: Boolean, initialUrl: String = "") {
        val normalized = normalizeUrlOrSearch(initialUrl)
        val initialTitle = when {
            normalized.isNotBlank() -> normalized
            isPrivate -> "Private Tab"
            else -> "Start Page"
        }
        val tabId = PrismEngine.createTab(
            isPrivate = isPrivate,
            initialUrl = normalized,
            initialTitle = initialTitle
        )
        syncStateFromEngine(activeOverrideId = tabId)
        _uiState.update {
            it.copy(
                isTabSheetOpen = false,
                isDiagnosticsSheetOpen = false,
                showStartPage = normalized.isBlank()
            )
        }
        emitEffect(BrowserEffect.SwitchWebView(tabId = tabId, isPrivate = isPrivate))
        if (normalized.isNotBlank()) {
            if (normalized == CaptureTestPage.INTERNAL_URL) {
                emitEffect(BrowserEffect.LoadCaptureTest(tabId = tabId))
            } else {
                emitEffect(BrowserEffect.LoadUrl(tabId = tabId, url = normalized))
            }
        }
    }

    fun selectTab(tabId: Long) {
        PrismEngine.setActiveTab(tabId)
        syncStateFromEngine(activeOverrideId = tabId)
        val active = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        _uiState.update { it.copy(isTabSheetOpen = false) }
        emitEffect(BrowserEffect.SwitchWebView(tabId = active.id, isPrivate = active.isPrivate))
    }

    fun toggleNormalPrivateMode() {
        val currentIsPrivate = _uiState.value.isActiveTabPrivate
        val targetPrivate = !currentIsPrivate
        val existingTarget = _uiState.value.tabs.lastOrNull { it.isPrivate == targetPrivate }
        if (existingTarget != null) {
            selectTab(existingTarget.id)
        } else {
            createNewTab(isPrivate = targetPrivate)
        }
    }

    fun closeTab(tabId: Long) {
        val wasPrivate = PrismEngine.closeTab(tabId)
        emitEffect(BrowserEffect.DestroyTabWebView(tabId = tabId, wipePrivateData = wasPrivate))

        if (wasPrivate) {
            emitEffect(BrowserEffect.ShowMessage("Private tab data cleared (FLAG_SECURE remains off)"))
        }

        val remaining = PrismEngine.getTabs()
        if (remaining.isEmpty()) {
            createNewTab(isPrivate = false)
        } else {
            syncStateFromEngine()
            val active = remaining.firstOrNull { it.isActive } ?: remaining.last()
            emitEffect(BrowserEffect.SwitchWebView(tabId = active.id, isPrivate = active.isPrivate))
        }
    }

    fun closeAllPrivateTabs() {
        val privateIds = _uiState.value.tabs.filter { it.isPrivate }.map { it.id }
        if (privateIds.isEmpty()) return

        PrismEngine.closeAllPrivateTabs()
        emitEffect(BrowserEffect.DestroyPrivateWebViews(privateIds))
        emitEffect(BrowserEffect.ShowMessage("All private tabs, cookies, and storage wiped"))

        val remaining = PrismEngine.getTabs()
        if (remaining.isEmpty()) {
            createNewTab(isPrivate = false)
        } else {
            syncStateFromEngine()
            val active = remaining.firstOrNull { it.isActive } ?: remaining.last()
            emitEffect(BrowserEffect.SwitchWebView(tabId = active.id, isPrivate = active.isPrivate))
        }
    }

    fun onWebViewStateChanged(
        tabId: Long,
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
        isLoading: Boolean,
        progress: Int
    ) {
        val cleanTitle = title.ifBlank { url.ifBlank { "New Tab" } }
        PrismEngine.updateTab(tabId, url, cleanTitle)

        _uiState.update { state ->
            val updatedTabs = PrismEngine.getTabs()
            if (state.activeTabId != tabId) {
                state.copy(tabs = updatedTabs, diagnostics = PrismEngine.getDiagnostics())
            } else {
                state.copy(
                    tabs = updatedTabs,
                    currentUrl = url,
                    addressInput = if (url.isNotBlank()) url else state.addressInput,
                    currentTitle = cleanTitle,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    isLoading = isLoading,
                    loadProgress = progress,
                    showStartPage = if (url.isNotBlank()) false else state.showStartPage,
                    diagnostics = PrismEngine.getDiagnostics()
                )
            }
        }
    }

    fun syncNavigationControls(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update {
            it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
        }
    }

    fun onBackClicked() = emitEffect(BrowserEffect.GoBack)
    fun onForwardClicked() = emitEffect(BrowserEffect.GoForward)
    fun onReloadOrStopClicked() {
        if (_uiState.value.isLoading) {
            emitEffect(BrowserEffect.StopLoading)
        } else {
            emitEffect(BrowserEffect.Reload)
        }
    }

    fun setTabSheetOpen(open: Boolean) {
        _uiState.update {
            it.copy(
                isTabSheetOpen = open,
                isDiagnosticsSheetOpen = if (open) false else it.isDiagnosticsSheetOpen
            )
        }
    }

    fun setDiagnosticsSheetOpen(open: Boolean, flagSecureCleared: Boolean? = null) {
        _uiState.update {
            it.copy(
                isDiagnosticsSheetOpen = open,
                isTabSheetOpen = if (open) false else it.isTabSheetOpen,
                flagSecureCleared = flagSecureCleared ?: it.flagSecureCleared,
                diagnostics = PrismEngine.getDiagnostics()
            )
        }
    }

    private fun syncTabsOnly() {
        _uiState.update {
            it.copy(
                tabs = PrismEngine.getTabs(),
                diagnostics = PrismEngine.getDiagnostics()
            )
        }
    }

    private fun syncStateFromEngine(activeOverrideId: Long? = null) {
        val tabs = PrismEngine.getTabs()
        val active = tabs.firstOrNull { tab ->
            if (activeOverrideId != null) tab.id == activeOverrideId else tab.isActive
        } ?: tabs.firstOrNull()

        _uiState.update { state ->
            state.copy(
                tabs = tabs,
                activeTabId = active?.id ?: -1L,
                isActiveTabPrivate = active?.isPrivate ?: false,
                currentUrl = active?.url.orEmpty(),
                addressInput = active?.url.orEmpty(),
                currentTitle = active?.title ?: "Start Page",
                showStartPage = active?.url.isNullOrBlank(),
                isLoading = false,
                loadProgress = 0,
                diagnostics = PrismEngine.getDiagnostics()
            )
        }
    }

    private fun emitEffect(effect: BrowserEffect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }
}
