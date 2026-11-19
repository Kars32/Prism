package com.example.core

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

/**
 * Layer 2B network interceptor for `WebViewClient.shouldInterceptRequest`.
 * Intercepts main-frame HTTP/HTTPS GET HTML documents, strips `display-capture`
 * restrictions from `Permissions-Policy` and `Feature-Policy` response headers via
 * the Rust core, and returns a clean `WebResourceResponse`.
 */
class CaptureSafeRequestInterceptor(
    private val onDiagnosticsUpdated: () -> Unit
) {
    companion object {
        private const val TAG = "PrismInterceptor"

        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    fun interceptMainFrameHtml(
        request: WebResourceRequest,
        userAgent: String,
        isPrivateTab: Boolean
    ): WebResourceResponse? {
        if (!request.isForMainFrame) return null
        if (!request.method.equals("GET", ignoreCase = true)) return null

        val uri = request.url ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null

        val urlString = uri.toString()
        if (urlString.startsWith("https://prism.internal/")) return null

        return try {
            val reqBuilder = Request.Builder()
                .url(urlString)
                .get()

            request.requestHeaders?.forEach { (key, value) ->
                if (!key.equals("Accept-Encoding", ignoreCase = true) &&
                    !key.equals("Cookie", ignoreCase = true)
                ) {
                    reqBuilder.header(key, value)
                }
            }
            reqBuilder.header("User-Agent", userAgent)

            val cookieManager = CookieManager.getInstance()
            if (!isPrivateTab) {
                cookieManager.getCookie(urlString)?.takeIf { it.isNotBlank() }?.let { cookie ->
                    reqBuilder.header("Cookie", cookie)
                }
            }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                // If redirected to a different origin/URL, let WebView handle navigation cleanly
                // so the address bar and relative asset resolution stay accurate.
                if (response.request.url.toString() != urlString) {
                    return null
                }

                val mediaType = response.body?.contentType()
                val mimeType = mediaType?.let { "${it.type}/${it.subtype}" }?.lowercase()
                    ?: response.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase()
                    ?: ""

                if (mimeType != "text/html" && mimeType != "application/xhtml+xml") {
                    return null
                }

                val charset = mediaType?.charset(Charsets.UTF_8)?.name() ?: "UTF-8"
                val rawBytes = response.body?.bytes() ?: ByteArray(0)

                val cleanHeaders = LinkedHashMap<String, String>()
                val responseHeaders = response.headers

                for (i in 0 until responseHeaders.size) {
                    val headerName = responseHeaders.name(i)
                    val headerValue = responseHeaders.value(i)

                    when {
                        headerName.equals("Set-Cookie", ignoreCase = true) -> {
                            if (!isPrivateTab) {
                                cookieManager.setCookie(urlString, headerValue)
                            }
                        }

                        headerName.equals("Content-Encoding", ignoreCase = true) ||
                            headerName.equals("Content-Length", ignoreCase = true) ||
                            headerName.equals("Transfer-Encoding", ignoreCase = true) -> {
                            // OkHttp already decoded transfer/gzip framing; omit stale framing headers.
                        }

                        headerName.equals("Permissions-Policy", ignoreCase = true) ||
                            headerName.equals("Feature-Policy", ignoreCase = true) -> {
                            val sanitized = PrismEngine.stripPermissionsPolicy(
                                headerName = headerName,
                                headerValue = headerValue,
                                url = urlString
                            )
                            if (sanitized.isNotBlank()) {
                                cleanHeaders[headerName] = sanitized
                            }
                        }

                        else -> {
                            cleanHeaders[headerName] = headerValue
                        }
                    }
                }

                PrismEngine.recordMainFrameIntercept()
                onDiagnosticsUpdated()

                val statusCode = response.code.takeIf { it in 100..599 } ?: 200
                val reasonPhrase = response.message.ifBlank { "OK" }

                WebResourceResponse(
                    mimeType,
                    charset,
                    statusCode,
                    reasonPhrase,
                    cleanHeaders,
                    ByteArrayInputStream(rawBytes)
                )
            }
        } catch (ex: Exception) {
            Log.d(TAG, "Main-frame intercept fallback for $urlString: ${ex.message}")
            null
        }
    }
}
