package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.browser.BrowserUiState
import com.example.browser.QuickLink
import com.example.ui.theme.PrismAlabaster
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismCyanContainer
import com.example.ui.theme.PrismEmerald
import com.example.ui.theme.PrismEmeraldContainer
import com.example.ui.theme.PrismObsidian
import com.example.ui.theme.PrismOutline
import com.example.ui.theme.PrismPewter
import com.example.ui.theme.PrismSlateMuted
import com.example.ui.theme.PrismSurface
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.PrismVioletContainer

@Composable
fun PrismStartPage(
    uiState: BrowserUiState,
    onQuickLinkClick: (QuickLink) -> Unit,
    onOpenDiagnosticsClick: () -> Unit,
    onNewPrivateTabClick: () -> Unit,
    onOpenTabSheetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeAccent = if (uiState.isActiveTabPrivate) PrismViolet else PrismCyan
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PrismObsidian)
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(scrollState)
                .padding(bottom = 28.dp)
        ) {
            // Atmospheric Prism Optical Glass Hero Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_prism_hero_1795092468524),
                    contentDescription = stringResource(R.string.cd_prism_hero),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrismObsidian.copy(alpha = 0.25f),
                                    PrismObsidian.copy(alpha = 0.65f),
                                    PrismObsidian
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PRISM",
                            style = MaterialTheme.typography.displaySmall,
                            color = PrismAlabaster
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.isActiveTabPrivate) PrismVioletContainer else PrismCyanContainer,
                            border = BorderStroke(1.dp, modeAccent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (uiState.isActiveTabPrivate) "PRIVATE" else "NORMAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = modeAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (uiState.isActiveTabPrivate) {
                            "Ephemeral session — storage wipes on tab close without FLAG_SECURE."
                        } else {
                            "Capture-safe browsing across screenshots, recordings, and live sharing."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrismPewter
                    )
                }
            }

            // Live Layer 1 & 2 Protection Status Card (tappable to open Diagnostics Sheet)
            Surface(
                onClick = onOpenDiagnosticsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("start_page_diagnostics_card"),
                shape = RoundedCornerShape(16.dp),
                color = PrismSurface,
                border = BorderStroke(1.dp, PrismEmerald.copy(alpha = 0.42f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrismEmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = PrismEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Capture Protection Active",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrismAlabaster
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val engineTag = if (uiState.diagnostics.nativeRustActive) "Rust JNI" else "Mirror Core"
                        Text(
                            text = "L1 Cleared • L2 Shim Ready • ${uiState.diagnostics.strippedHeaders} policies stripped • $engineTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrismEmerald,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = PrismPewter,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Quick Launch Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Launch",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrismAlabaster
                )
                Text(
                    text = "${uiState.tabs.size} open ${if (uiState.tabs.size == 1) "tab" else "tabs"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrismCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenTabSheetClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("start_page_tabs_link")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2-Column Quick Launch Grid
            val rows = uiState.quickLinks.chunked(2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { link ->
                            QuickLinkCard(
                                link = link,
                                onClick = { onQuickLinkClick(link) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Private Tab Launch Action Card
            if (!uiState.isActiveTabPrivate) {
                Surface(
                    onClick = onNewPrivateTabClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("start_page_new_private_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = PrismVioletContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, PrismViolet.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = PrismViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Launch Capture-Safe Private Tab",
                                style = MaterialTheme.typography.titleSmall,
                                color = PrismAlabaster
                            )
                            Text(
                                text = "Clears cookies, cache & WebStorage on close",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrismPewter
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = PrismViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLinkCard(
    link: QuickLink,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (link.isDiagnostic) PrismEmerald else PrismCyan
    val badgeBg = if (link.isDiagnostic) PrismEmeraldContainer else Color(0xFF132231)

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(86.dp)
            .testTag("quick_link_${link.title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        color = PrismSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (link.isDiagnostic) PrismEmerald.copy(alpha = 0.45f) else PrismOutline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (link.isDiagnostic) Icons.Rounded.Science else Icons.Rounded.Public,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = PrismAlabaster,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = link.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrismSlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
