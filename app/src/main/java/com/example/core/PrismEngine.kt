package com.example.core

import org.json.JSONArray
import org.json.JSONObject

data class TabMetadata(
    val id: Long,
    val url: String,
    val title: String,
    val isPrivate: Boolean,
    val isActive: Boolean
)

data class SanitizedHeaderEvent(
    val url: String,
    val headerName: String,
    val removedDirective: String
)

data class CoreDiagnostics(
    val nativeRustActive: Boolean,
    val interceptedDocs: Long,
    val strippedHeaders: Long,
    val normalTabCount: Int,
    val privateTabCount: Int,
    val recentEvents: List<SanitizedHeaderEvent>
)

/**
 * Thread-safe engine facade backed by Rust JNI (`libprism_core.so`), with an identical
 * synchronized Kotlin mirror when built in environments without the Rust cargo-ndk toolchain.
 */
object PrismEngine {

    private const val MAX_RECENT_EVENTS = 20

    val isNativeRustActive: Boolean
        get() = NativePrismCore.isNativeLoaded

    fun getDocumentStartShim(): String {
        return if (NativePrismCore.isNativeLoaded) {
            NativePrismCore.getDocumentStartShim()
        } else {
            MIRROR_DOCUMENT_START_SHIM_JS
        }
    }

    fun stripPermissionsPolicy(headerName: String, headerValue: String, url: String): String {
        return if (NativePrismCore.isNativeLoaded) {
            NativePrismCore.stripPermissionsPolicy(headerName, headerValue, url)
        } else {
            MirrorState.stripPermissionsPolicy(headerName, headerValue, url)
        }
    }

    fun recordMainFrameIntercept() {
        if (NativePrismCore.isNativeLoaded) {
            NativePrismCore.recordMainFrameIntercept()
        } else {
            MirrorState.recordMainFrameIntercept()
        }
    }

    fun createTab(isPrivate: Boolean, initialUrl: String = "", initialTitle: String = "New Tab"): Long {
        return if (NativePrismCore.isNativeLoaded) {
            NativePrismCore.createTab(isPrivate, initialUrl, initialTitle)
        } else {
            MirrorState.createTab(isPrivate, initialUrl, initialTitle)
        }
    }

    fun updateTab(tabId: Long, url: String, title: String) {
        if (NativePrismCore.isNativeLoaded) {
            NativePrismCore.updateTab(tabId, url, title)
        } else {
            MirrorState.updateTab(tabId, url, title)
        }
    }

    fun setActiveTab(tabId: Long) {
        if (NativePrismCore.isNativeLoaded) {
            NativePrismCore.setActiveTab(tabId)
        } else {
            MirrorState.setActiveTab(tabId)
        }
    }

    fun closeTab(tabId: Long): Boolean {
        return if (NativePrismCore.isNativeLoaded) {
            NativePrismCore.closeTab(tabId)
        } else {
            MirrorState.closeTab(tabId)
        }
    }

    fun closeAllPrivateTabs(): Int {
        return if (NativePrismCore.isNativeLoaded) {
            NativePrismCore.closeAllPrivateTabs()
        } else {
            MirrorState.closeAllPrivateTabs()
        }
    }

    fun getTabs(): List<TabMetadata> {
        if (!NativePrismCore.isNativeLoaded) {
            return MirrorState.getTabs()
        }
        val raw = NativePrismCore.getTabsJson()
        val array = JSONArray(raw)
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    TabMetadata(
                        id = obj.getLong("id"),
                        url = obj.optString("url", ""),
                        title = obj.optString("title", "New Tab"),
                        isPrivate = obj.optBoolean("isPrivate", false),
                        isActive = obj.optBoolean("isActive", false)
                    )
                )
            }
        }
    }

    fun getDiagnostics(): CoreDiagnostics {
        if (!NativePrismCore.isNativeLoaded) {
            return MirrorState.getDiagnostics()
        }
        val obj = JSONObject(NativePrismCore.getDiagnosticsJson())
        val eventsArr = obj.optJSONArray("recentEvents") ?: JSONArray()
        val events = buildList(eventsArr.length()) {
            for (i in 0 until eventsArr.length()) {
                val item = eventsArr.getJSONObject(i)
                add(
                    SanitizedHeaderEvent(
                        url = item.optString("url", ""),
                        headerName = item.optString("headerName", ""),
                        removedDirective = item.optString("removedDirective", "")
                    )
                )
            }
        }
        return CoreDiagnostics(
            nativeRustActive = true,
            interceptedDocs = obj.optLong("interceptedDocs", 0L),
            strippedHeaders = obj.optLong("strippedHeaders", 0L),
            normalTabCount = obj.optInt("normalTabCount", 0),
            privateTabCount = obj.optInt("privateTabCount", 0),
            recentEvents = events
        )
    }

    private object MirrorState {
        private data class MutableTab(
            val id: Long,
            var url: String,
            var title: String,
            val isPrivate: Boolean
        )

        private val lock = Any()
        private var nextTabId: Long = 1L
        private val tabs = mutableListOf<MutableTab>()
        private var activeTabId: Long? = null
        private var interceptedDocs: Long = 0L
        private var strippedHeaders: Long = 0L
        private val recentEvents = ArrayDeque<SanitizedHeaderEvent>()

        fun createTab(isPrivate: Boolean, initialUrl: String, initialTitle: String): Long =
            synchronized(lock) {
                val id = nextTabId++
                tabs.add(MutableTab(id, initialUrl, initialTitle, isPrivate))
                activeTabId = id
                id
            }

        fun updateTab(tabId: Long, url: String, title: String) = synchronized(lock) {
            tabs.find { it.id == tabId }?.let {
                it.url = url
                it.title = title
            }
        }

        fun setActiveTab(tabId: Long) = synchronized(lock) {
            if (tabs.any { it.id == tabId }) {
                activeTabId = tabId
            }
        }

        fun closeTab(tabId: Long): Boolean = synchronized(lock) {
            val index = tabs.indexOfFirst { it.id == tabId }
            if (index == -1) return false
            val removed = tabs.removeAt(index)
            if (activeTabId == tabId) {
                activeTabId = tabs.lastOrNull { it.isPrivate == removed.isPrivate }?.id
                    ?: tabs.lastOrNull()?.id
            }
            removed.isPrivate
        }

        fun closeAllPrivateTabs(): Int = synchronized(lock) {
            val before = tabs.size
            tabs.removeAll { it.isPrivate }
            val removed = before - tabs.size
            if (tabs.none { it.id == activeTabId }) {
                activeTabId = tabs.lastOrNull()?.id
            }
            removed
        }

        fun recordMainFrameIntercept() = synchronized(lock) {
            interceptedDocs += 1L
        }

        fun stripPermissionsPolicy(headerName: String, headerValue: String, url: String): String {
            val isFeaturePolicy = headerName.equals("feature-policy", ignoreCase = true)
            val delimiter = if (isFeaturePolicy) ';' else ','
            val kept = mutableListOf<String>()
            var removedDirective: String? = null

            for (rawPart in headerValue.split(delimiter)) {
                val part = rawPart.trim()
                if (part.isEmpty()) continue
                val key = if (isFeaturePolicy) {
                    part.split(Regex("\\s+")).firstOrNull().orEmpty()
                } else {
                    part.substringBefore('=').trim()
                }
                if (key.equals("display-capture", ignoreCase = true)) {
                    removedDirective = part
                } else {
                    kept.add(part)
                }
            }

            if (removedDirective != null) {
                synchronized(lock) {
                    strippedHeaders += 1L
                    if (recentEvents.size >= MAX_RECENT_EVENTS) {
                        recentEvents.removeFirst()
                    }
                    recentEvents.addLast(
                        SanitizedHeaderEvent(
                            url = url,
                            headerName = headerName,
                            removedDirective = removedDirective
                        )
                    )
                }
            }

            val joiner = if (isFeaturePolicy) "; " else ", "
            return kept.joinToString(joiner)
        }

        fun getTabs(): List<TabMetadata> = synchronized(lock) {
            tabs.map {
                TabMetadata(
                    id = it.id,
                    url = it.url,
                    title = it.title,
                    isPrivate = it.isPrivate,
                    isActive = it.id == activeTabId
                )
            }
        }

        fun getDiagnostics(): CoreDiagnostics = synchronized(lock) {
            CoreDiagnostics(
                nativeRustActive = false,
                interceptedDocs = interceptedDocs,
                strippedHeaders = strippedHeaders,
                normalTabCount = tabs.count { !it.isPrivate },
                privateTabCount = tabs.count { it.isPrivate },
                recentEvents = recentEvents.reversed()
            )
        }
    }

    private const val MIRROR_DOCUMENT_START_SHIM_JS = """(function() {
  'use strict';
  if (window.__PRISM_CAPTURE_SHIM_ACTIVE__) {
    return;
  }
  Object.defineProperty(window, '__PRISM_CAPTURE_SHIM_ACTIVE__', {
    value: true,
    configurable: false,
    enumerable: false,
    writable: false
  });

  var defineGetter = function(target, prop, value) {
    try {
      Object.defineProperty(target, prop, {
        get: function() { return value; },
        configurable: true
      });
    } catch (_) {}
  };

  if (typeof Document !== 'undefined' && Document.prototype) {
    defineGetter(Document.prototype, 'visibilityState', 'visible');
    defineGetter(Document.prototype, 'webkitVisibilityState', 'visible');
    defineGetter(Document.prototype, 'hidden', false);
    defineGetter(Document.prototype, 'webkitHidden', false);
    try {
      Object.defineProperty(Document.prototype, 'hasFocus', {
        value: function() { return true; },
        configurable: true,
        writable: true
      });
    } catch (_) {}
  }

  defineGetter(document, 'visibilityState', 'visible');
  defineGetter(document, 'webkitVisibilityState', 'visible');
  defineGetter(document, 'hidden', false);
  defineGetter(document, 'webkitHidden', false);
  try {
    Object.defineProperty(document, 'hasFocus', {
      value: function() { return true; },
      configurable: true,
      writable: true
    });
  } catch (_) {}

  var BLOCKED_DOC_EVENTS = {
    visibilitychange: true,
    webkitvisibilitychange: true,
    mozvisibilitychange: true,
    msvisibilitychange: true,
    pagehide: true,
    freeze: true
  };

  var BLOCKED_WINDOW_BLUR_EVENTS = {
    blur: true,
    focusout: true
  };

  var isGlobalTarget = function(target) {
    return target === window || target === document || target === document.documentElement;
  };

  var origAddEventListener = EventTarget.prototype.addEventListener;
  EventTarget.prototype.addEventListener = function(type, listener, options) {
    var lower = typeof type === 'string' ? type.toLowerCase() : '';
    if (BLOCKED_DOC_EVENTS[lower]) {
      return;
    }
    if (BLOCKED_WINDOW_BLUR_EVENTS[lower] && isGlobalTarget(this)) {
      return;
    }
    return origAddEventListener.call(this, type, listener, options);
  };

  var suppressVisibilityEvent = function(event) {
    event.stopImmediatePropagation();
  };

  var suppressGlobalBlurEvent = function(event) {
    var target = event.target;
    if (!target || isGlobalTarget(target)) {
      event.stopImmediatePropagation();
    }
  };

  ['visibilitychange', 'webkitvisibilitychange', 'mozvisibilitychange', 'msvisibilitychange', 'pagehide', 'freeze']
    .forEach(function(evtName) {
      origAddEventListener.call(window, evtName, suppressVisibilityEvent, true);
      origAddEventListener.call(document, evtName, suppressVisibilityEvent, true);
    });

  ['blur', 'focusout'].forEach(function(evtName) {
    origAddEventListener.call(window, evtName, suppressGlobalBlurEvent, true);
    origAddEventListener.call(document, evtName, suppressGlobalBlurEvent, true);
  });

  var neutralizeHandler = function(target, prop) {
    try {
      Object.defineProperty(target, prop, {
        get: function() { return null; },
        set: function() {},
        configurable: true
      });
    } catch (_) {}
  };

  neutralizeHandler(document, 'onvisibilitychange');
  neutralizeHandler(document, 'onwebkitvisibilitychange');
  neutralizeHandler(window, 'onblur');
  neutralizeHandler(document, 'onblur');
})();"""
}
