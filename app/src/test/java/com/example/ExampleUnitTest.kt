package com.example

import com.example.core.PrismEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun documentStartShimOverridesVisibilityAndFocus() {
        val shim = PrismEngine.getDocumentStartShim()
        assertTrue(shim.contains("visibilityState"))
        assertTrue(shim.contains("'visible'"))
        assertTrue(shim.contains("'hidden'"))
        assertTrue(shim.contains("hasFocus"))
        assertTrue(shim.contains("visibilitychange"))
        assertTrue(shim.contains("focusout"))
    }

    @Test
    fun stripsDisplayCaptureFromPermissionsPolicy() {
        val cleaned = PrismEngine.stripPermissionsPolicy(
            headerName = "Permissions-Policy",
            headerValue = "camera=(self), display-capture=(), microphone=()",
            url = "https://example.com"
        )
        assertEquals("camera=(self), microphone=()", cleaned)
    }

    @Test
    fun stripsDisplayCaptureFromFeaturePolicy() {
        val cleaned = PrismEngine.stripPermissionsPolicy(
            headerName = "Feature-Policy",
            headerValue = "display-capture 'none'; fullscreen 'self'",
            url = "https://example.com"
        )
        assertEquals("fullscreen 'self'", cleaned)
    }

    @Test
    fun tabLifecycleTracksPrivateFlagsCorrectly() {
        val normalId = PrismEngine.createTab(false, "https://example.com", "Example")
        val privateId = PrismEngine.createTab(true, "https://duckduckgo.com", "DuckDuckGo")

        assertTrue(PrismEngine.getTabs().any { it.id == normalId && !it.isPrivate })
        assertTrue(PrismEngine.getTabs().any { it.id == privateId && it.isPrivate })

        val wasPrivate = PrismEngine.closeTab(privateId)
        assertTrue(wasPrivate)
        assertFalse(PrismEngine.closeTab(normalId))
    }
}
