use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jlong, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use std::collections::VecDeque;
use std::sync::{Mutex, OnceLock};

const MAX_RECENT_EVENTS: usize = 20;

/// Layer 2 Document-Start JavaScript shim.
/// Injected via WebViewCompat.addDocumentStartJavaScript with "*" allowed origin rules
/// so it executes before any page or iframe script.
pub const DOCUMENT_START_SHIM_JS: &str = r#"(function() {
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

  // Override visibility and focus APIs on both Document.prototype and document instance.
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

  // Intercept addEventListener so pages cannot register visibility/blur obfuscation hooks.
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

  // Suppress capture-phase propagation for visibilitychange, blur, and focusout on window/document
  // while leaving form input blur/focusout intact for normal text editing.
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

  // Neutralize inline onvisibilitychange / onblur assignment handlers.
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
})();"#;

#[derive(Clone, Debug)]
struct TabRecord {
    id: u64,
    url: String,
    title: String,
    is_private: bool,
}

#[derive(Clone, Debug)]
struct SanitizedEvent {
    url: String,
    header_name: String,
    removed_directive: String,
}

struct BrowserState {
    next_tab_id: u64,
    tabs: Vec<TabRecord>,
    active_tab_id: Option<u64>,
    intercepted_docs: u64,
    stripped_headers: u64,
    recent_events: VecDeque<SanitizedEvent>,
}

impl Default for BrowserState {
    fn default() -> Self {
        Self {
            next_tab_id: 1,
            tabs: Vec::new(),
            active_tab_id: None,
            intercepted_docs: 0,
            stripped_headers: 0,
            recent_events: VecDeque::with_capacity(MAX_RECENT_EVENTS),
        }
    }
}

static STATE: OnceLock<Mutex<BrowserState>> = OnceLock::new();

fn state_lock() -> &'static Mutex<BrowserState> {
    STATE.get_or_init(|| Mutex::new(BrowserState::default()))
}

fn escape_json(input: &str) -> String {
    let mut out = String::with_capacity(input.len() + 8);
    for ch in input.chars() {
        match ch {
            '"' => out.push_str("\\\""),
            '\\' => out.push_str("\\\\"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            '\t' => out.push_str("\\t"),
            c if (c as u32) < 0x20 => {
                out.push_str(&format!("\\u{:04x}", c as u32));
            }
            c => out.push(c),
        }
    }
    out
}

/// Strips `display-capture` restrictions from `Permissions-Policy` (comma-separated)
/// or legacy `Feature-Policy` (semicolon-separated) headers.
/// Returns `(cleaned_header_value, removed_directive_if_any)`.
pub fn strip_display_capture_policy(header_name: &str, header_value: &str) -> (String, Option<String>) {
    let is_feature_policy = header_name.eq_ignore_ascii_case("feature-policy");
    let delimiter = if is_feature_policy { ';' } else { ',' };

    let mut kept: Vec<&str> = Vec::new();
    let mut removed: Option<String> = None;

    for raw_part in header_value.split(delimiter) {
        let part = raw_part.trim();
        if part.is_empty() {
            continue;
        }
        let directive_key = if is_feature_policy {
            part.split_whitespace().next().unwrap_or("")
        } else {
            part.split('=').next().unwrap_or("").trim()
        };

        if directive_key.eq_ignore_ascii_case("display-capture") {
            removed = Some(part.to_string());
        } else {
            kept.push(part);
        }
    }

    let joiner = if is_feature_policy { "; " } else { ", " };
    (kept.join(joiner), removed)
}

fn read_jstring(env: &mut JNIEnv, value: JString) -> String {
    env.get_string(&value)
        .map(|s| s.into())
        .unwrap_or_default()
}

fn to_jstring(env: &JNIEnv, value: &str) -> jstring {
    env.new_string(value)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_getDocumentStartShim(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    to_jstring(&env, DOCUMENT_START_SHIM_JS)
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_stripPermissionsPolicy(
    mut env: JNIEnv,
    _class: JClass,
    header_name: JString,
    header_value: JString,
    url: JString,
) -> jstring {
    let name = read_jstring(&mut env, header_name);
    let value = read_jstring(&mut env, header_value);
    let page_url = read_jstring(&mut env, url);

    let (cleaned, removed) = strip_display_capture_policy(&name, &value);

    if let Some(removed_directive) = removed {
        if let Ok(mut guard) = state_lock().lock() {
            guard.stripped_headers += 1;
            if guard.recent_events.len() >= MAX_RECENT_EVENTS {
                guard.recent_events.pop_front();
            }
            guard.recent_events.push_back(SanitizedEvent {
                url: page_url,
                header_name: name,
                removed_directive,
            });
        }
    }

    to_jstring(&env, &cleaned)
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_recordMainFrameIntercept(
    _env: JNIEnv,
    _class: JClass,
) {
    if let Ok(mut guard) = state_lock().lock() {
        guard.intercepted_docs += 1;
    }
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_createTab(
    mut env: JNIEnv,
    _class: JClass,
    is_private: jboolean,
    initial_url: JString,
    initial_title: JString,
) -> jlong {
    let url = read_jstring(&mut env, initial_url);
    let title = read_jstring(&mut env, initial_title);
    let private_flag = is_private == JNI_TRUE;

    if let Ok(mut guard) = state_lock().lock() {
        let id = guard.next_tab_id;
        guard.next_tab_id += 1;
        guard.tabs.push(TabRecord {
            id,
            url,
            title,
            is_private: private_flag,
        });
        guard.active_tab_id = Some(id);
        return id as jlong;
    }
    -1
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_updateTab(
    mut env: JNIEnv,
    _class: JClass,
    tab_id: jlong,
    url: JString,
    title: JString,
) {
    let new_url = read_jstring(&mut env, url);
    let new_title = read_jstring(&mut env, title);
    let id = tab_id as u64;

    if let Ok(mut guard) = state_lock().lock() {
        if let Some(tab) = guard.tabs.iter_mut().find(|t| t.id == id) {
            tab.url = new_url;
            tab.title = new_title;
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_setActiveTab(
    _env: JNIEnv,
    _class: JClass,
    tab_id: jlong,
) {
    let id = tab_id as u64;
    if let Ok(mut guard) = state_lock().lock() {
        if guard.tabs.iter().any(|t| t.id == id) {
            guard.active_tab_id = Some(id);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_closeTab(
    _env: JNIEnv,
    _class: JClass,
    tab_id: jlong,
) -> jboolean {
    let id = tab_id as u64;
    if let Ok(mut guard) = state_lock().lock() {
        if let Some(idx) = guard.tabs.iter().position(|t| t.id == id) {
            let removed = guard.tabs.remove(idx);
            if guard.active_tab_id == Some(id) {
                guard.active_tab_id = guard
                    .tabs
                    .iter()
                    .rev()
                    .find(|t| t.is_private == removed.is_private)
                    .or_else(|| guard.tabs.last())
                    .map(|t| t.id);
            }
            return if removed.is_private {
                JNI_TRUE
            } else {
                JNI_FALSE
            };
        }
    }
    JNI_FALSE
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_closeAllPrivateTabs(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    if let Ok(mut guard) = state_lock().lock() {
        let before = guard.tabs.len();
        guard.tabs.retain(|t| !t.is_private);
        let removed_count = before.saturating_sub(guard.tabs.len());

        let active_still_exists = guard
            .active_tab_id
            .map(|aid| guard.tabs.iter().any(|t| t.id == aid))
            .unwrap_or(false);
        if !active_still_exists {
            guard.active_tab_id = guard.tabs.last().map(|t| t.id);
        }
        return removed_count as jint;
    }
    0
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_getTabsJson(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let json = if let Ok(guard) = state_lock().lock() {
        let active_id = guard.active_tab_id;
        let items: Vec<String> = guard
            .tabs
            .iter()
            .map(|t| {
                format!(
                    r#"{{"id":{},"url":"{}","title":"{}","isPrivate":{},"isActive":{}}}"#,
                    t.id,
                    escape_json(&t.url),
                    escape_json(&t.title),
                    t.is_private,
                    Some(t.id) == active_id
                )
            })
            .collect();
        format!("[{}]", items.join(","))
    } else {
        "[]".to_string()
    };

    to_jstring(&env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativePrismCore_getDiagnosticsJson(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let json = if let Ok(guard) = state_lock().lock() {
        let normal_count = guard.tabs.iter().filter(|t| !t.is_private).count();
        let private_count = guard.tabs.iter().filter(|t| t.is_private).count();
        let events: Vec<String> = guard
            .recent_events
            .iter()
            .rev()
            .map(|e| {
                format!(
                    r#"{{"url":"{}","headerName":"{}","removedDirective":"{}"}}"#,
                    escape_json(&e.url),
                    escape_json(&e.header_name),
                    escape_json(&e.removed_directive)
                )
            })
            .collect();

        format!(
            r#"{{"interceptedDocs":{},"strippedHeaders":{},"normalTabCount":{},"privateTabCount":{},"recentEvents":[{}]}}"#,
            guard.intercepted_docs,
            guard.stripped_headers,
            normal_count,
            private_count,
            events.join(",")
        )
    } else {
        r#"{"interceptedDocs":0,"strippedHeaders":0,"normalTabCount":0,"privateTabCount":0,"recentEvents":[]}"#
            .to_string()
    };

    to_jstring(&env, &json)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn strips_display_capture_from_permissions_policy() {
        let (cleaned, removed) = strip_display_capture_policy(
            "Permissions-Policy",
            "camera=(self), display-capture=(), microphone=()",
        );
        assert_eq!(cleaned, "camera=(self), microphone=()");
        assert_eq!(removed.as_deref(), Some("display-capture=()"));
    }

    #[test]
    fn strips_display_capture_from_feature_policy() {
        let (cleaned, removed) = strip_display_capture_policy(
            "Feature-Policy",
            "display-capture 'none'; fullscreen 'self'",
        );
        assert_eq!(cleaned, "fullscreen 'self'");
        assert_eq!(removed.as_deref(), Some("display-capture 'none'"));
    }

    #[test]
    fn shim_overrides_visibility_and_focus() {
        assert!(DOCUMENT_START_SHIM_JS.contains("visibilityState"));
        assert!(DOCUMENT_START_SHIM_JS.contains("hasFocus"));
        assert!(DOCUMENT_START_SHIM_JS.contains("visibilitychange"));
        assert!(DOCUMENT_START_SHIM_JS.contains("focusout"));
    }
}
