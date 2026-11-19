package com.example.core

/**
 * Built-in local diagnostic page loaded inside WebView so the user can live-verify
 * Layer 2 document-start visibility/focus overrides and event suppression.
 */
object CaptureTestPage {
    const val INTERNAL_URL = "https://prism.internal/capture-test"

    fun buildHtml(flagSecureCleared: Boolean, nativeRustLoaded: Boolean): String {
        val layer1Status = if (flagSecureCleared) "CLEARED (PASS)" else "SET (FAIL)"
        val engineLabel = if (nativeRustLoaded) "Rust JNI (libprism_core.so)" else "Kotlin Mirror Engine"
        return """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Prism Live Capture Self-Test</title>
  <style>
    :root {
      color-scheme: dark;
      --bg: #0F1216;
      --card: #171C23;
      --border: #28313E;
      --text: #F1F5F9;
      --muted: #94A3B8;
      --cyan: #38BDF8;
      --emerald: #34D399;
      --rose: #FB7185;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 20px 16px 40px;
      background: var(--bg);
      color: var(--text);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      line-height: 1.45;
    }
    h1 {
      font-size: 20px;
      margin: 0 0 6px;
      letter-spacing: -0.02em;
    }
    .subtitle {
      color: var(--muted);
      font-size: 13px;
      margin: 0 0 18px;
    }
    .card {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 14px;
      margin-bottom: 12px;
    }
    .row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
      border-bottom: 1px solid #1E2530;
      gap: 12px;
    }
    .row:last-child { border-bottom: none; }
    .label {
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 12px;
      color: var(--muted);
    }
    .val {
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 12px;
      font-weight: 600;
    }
    .pass { color: var(--emerald); }
    .fail { color: var(--rose); }
    button {
      width: 100%;
      padding: 13px 16px;
      border-radius: 12px;
      border: 1px solid #38BDF866;
      background: #0C2B3E;
      color: var(--cyan);
      font-weight: 600;
      font-size: 14px;
      cursor: pointer;
      margin-top: 6px;
    }
    #log {
      margin-top: 10px;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 12px;
      color: var(--emerald);
      white-space: pre-wrap;
    }
  </style>
</head>
<body>
  <h1>Prism Live Capture Self-Test</h1>
  <p class="subtitle">Verifying Layer 1 window flags and Layer 2 document-start JavaScript overrides inside Android System WebView.</p>

  <div class="card">
    <div class="row"><span class="label">Layer 1 · FLAG_SECURE</span><span class="val ${if (flagSecureCleared) "pass" else "fail"}">$layer1Status</span></div>
    <div class="row"><span class="label">Core Runtime</span><span class="val pass">$engineLabel</span></div>
    <div class="row"><span class="label">Shim Flag Active</span><span id="shimActive" class="val">checking...</span></div>
    <div class="row"><span class="label">document.visibilityState</span><span id="visState" class="val">checking...</span></div>
    <div class="row"><span class="label">document.hidden</span><span id="docHidden" class="val">checking...</span></div>
    <div class="row"><span class="label">document.hasFocus()</span><span id="hasFocus" class="val">checking...</span></div>
  </div>

  <div class="card">
    <div class="label" style="margin-bottom:8px;">Event Suppression Verification</div>
    <button id="testEventsBtn" type="button">Dispatch visibilitychange, blur &amp; focusout</button>
    <div id="log">Tap the button to verify hostile page hide listeners cannot fire.</div>
  </div>

  <script>
    (function() {
      var setStatus = function(id, text, ok) {
        var el = document.getElementById(id);
        el.textContent = text;
        el.className = 'val ' + (ok ? 'pass' : 'fail');
      };

      var shimOk = window.__PRISM_CAPTURE_SHIM_ACTIVE__ === true;
      var visOk = document.visibilityState === 'visible';
      var hiddenOk = document.hidden === false;
      var focusOk = document.hasFocus() === true;

      setStatus('shimActive', String(shimOk), shimOk);
      setStatus('visState', "'" + document.visibilityState + "'", visOk);
      setStatus('docHidden', String(document.hidden), hiddenOk);
      setStatus('hasFocus', String(document.hasFocus()), focusOk);

      var leakedEvents = [];
      document.addEventListener('visibilitychange', function() { leakedEvents.push('visibilitychange'); });
      window.addEventListener('blur', function() { leakedEvents.push('window.blur'); });
      window.addEventListener('focusout', function() { leakedEvents.push('window.focusout'); });

      document.getElementById('testEventsBtn').addEventListener('click', function() {
        leakedEvents = [];
        document.dispatchEvent(new Event('visibilitychange'));
        window.dispatchEvent(new FocusEvent('blur'));
        window.dispatchEvent(new FocusEvent('focusout'));
        var logEl = document.getElementById('log');
        if (leakedEvents.length === 0) {
          logEl.style.color = '#34D399';
          logEl.textContent = 'PASS: 0 hide/blur callbacks leaked. Page remains fully visible during capture.';
        } else {
          logEl.style.color = '#FB7185';
          logEl.textContent = 'FAIL: Leaked events -> ' + leakedEvents.join(', ');
        }
      });
    })();
  </script>
</body>
</html>"""
    }
}
