package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.BrowserUiState
import com.example.ui.theme.PrismAlabaster
import com.example.ui.theme.PrismAmber
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismCyanContainer
import com.example.ui.theme.PrismEmerald
import com.example.ui.theme.PrismEmeraldContainer
import com.example.ui.theme.PrismObsidian
import com.example.ui.theme.PrismOutline
import com.example.ui.theme.PrismPewter
import com.example.ui.theme.PrismRose
import com.example.ui.theme.PrismSlateMuted
import com.example.ui.theme.PrismSurface
import com.example.ui.theme.PrismViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionDiagnosticsSheet(
    uiState: BrowserUiState,
    onDismiss: () -> Unit,
    onRunLiveCaptureTest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val diag = uiState.diagnostics

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PrismObsidian,
        contentColor = PrismAlabaster,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PrismOutline)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = PrismEmerald,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Capture Protection Status",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PrismAlabaster
                    )
                    Text(
                        text = "Live Layer 1 & Layer 2 verification telemetry",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrismPewter
                    )
                }
            }

            // Layer 1 Card
            DiagnosticLayerCard(
                title = "Layer 1 · Window Flag Control",
                badgeText = if (uiState.flagSecureCleared) "CLEARED" else "BLOCKED",
                badgeColor = if (uiState.flagSecureCleared) PrismEmerald else PrismRose,
                detail = "WindowManager.LayoutParams.FLAG_SECURE is explicitly cleared in onCreate() and onResume() across Normal and Private tabs."
            )

            // Layer 2A Card
            DiagnosticLayerCard(
                title = "Layer 2A · Document-Start JS Shim",
                badgeText = if (uiState.documentStartScriptSupported) "INJECTED" else "UNSUPPORTED",
                badgeColor = if (uiState.documentStartScriptSupported) PrismEmerald else PrismAmber,
                detail = "WebViewCompat.addDocumentStartJavaScript (origin \"*\") locks visibilityState='visible', hidden=false, hasFocus()=true and suppresses visibilitychange, blur & focusout."
            )

            // Layer 2B Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PrismSurface,
                border = BorderStroke(1.dp, PrismOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Layer 2B · HTTP Policy Sanitizer",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrismAlabaster
                        )
                        StatusPill(text = "ACTIVE", color = PrismEmerald)
                    }

                    Text(
                        text = "Intercepts main-frame HTML responses and removes display-capture restrictions from Permissions-Policy and Feature-Policy headers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrismPewter
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricChip(
                            label = "HTML Intercepted",
                            value = diag.interceptedDocs.toString(),
                            accent = PrismCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricChip(
                            label = "Policies Stripped",
                            value = diag.strippedHeaders.toString(),
                            accent = PrismEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (diag.recentEvents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Recent Stripped Directives",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrismPewter
                        )
                        diag.recentEvents.take(4).forEach { event ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrismObsidian)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${event.headerName}: ${event.removedDirective}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrismEmerald,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = event.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrismSlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Runtime Engine & Inspector Info
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PrismSurface,
                border = BorderStroke(1.dp, PrismOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = if (diag.nativeRustActive) PrismEmerald else PrismViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (diag.nativeRustActive) {
                                "Core Runtime: Rust JNI (libprism_core.so)"
                            } else {
                                "Core Runtime: Kotlin Mirror (cargo-ndk not on build host)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = PrismAlabaster
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = null,
                            tint = if (uiState.webContentsDebuggingEnabled) PrismCyan else PrismSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (uiState.webContentsDebuggingEnabled) {
                                "WebView Debugging: Enabled (chrome://inspect/#devices)"
                            } else {
                                "WebView Debugging: Release build (disabled)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = PrismPewter
                        )
                    }
                }
            }

            // Live Verification Button
            Button(
                onClick = onRunLiveCaptureTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("run_capture_self_test_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrismCyanContainer,
                    contentColor = PrismCyan
                ),
                border = BorderStroke(1.dp, PrismCyan.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Science,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Run Live Capture Self-Test in WebView",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DiagnosticLayerCard(
    title: String,
    badgeText: String,
    badgeColor: Color,
    detail: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PrismSurface,
        border = BorderStroke(1.dp, PrismOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = PrismAlabaster
                )
                StatusPill(text = badgeText, color = badgeColor)
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = PrismPewter
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (color == PrismEmerald) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PrismObsidian)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PrismPewter
        )
    }
}
