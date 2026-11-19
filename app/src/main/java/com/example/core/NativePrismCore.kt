package com.example.core

import android.util.Log

/**
 * Direct JNI bridge into `libprism_core.so` compiled from `rust/src/lib.rs` via cargo-ndk.
 */
object NativePrismCore {
    private const val TAG = "NativePrismCore"

    val isNativeLoaded: Boolean = try {
        System.loadLibrary("prism_core")
        true
    } catch (error: UnsatisfiedLinkError) {
        runCatching {
            Log.w(TAG, "libprism_core.so not packaged in this build; using Kotlin mirror fallback.", error)
        }
        false
    } catch (error: SecurityException) {
        runCatching {
            Log.w(TAG, "Unable to load libprism_core.so; using Kotlin mirror fallback.", error)
        }
        false
    }

    @JvmStatic
    external fun getDocumentStartShim(): String

    @JvmStatic
    external fun stripPermissionsPolicy(
        headerName: String,
        headerValue: String,
        url: String
    ): String

    @JvmStatic
    external fun recordMainFrameIntercept()

    @JvmStatic
    external fun createTab(
        isPrivate: Boolean,
        initialUrl: String,
        initialTitle: String
    ): Long

    @JvmStatic
    external fun updateTab(
        tabId: Long,
        url: String,
        title: String
    )

    @JvmStatic
    external fun setActiveTab(tabId: Long)

    @JvmStatic
    external fun closeTab(tabId: Long): Boolean

    @JvmStatic
    external fun closeAllPrivateTabs(): Int

    @JvmStatic
    external fun getTabsJson(): String

    @JvmStatic
    external fun getDiagnosticsJson(): String
}
