package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.browser.BrowserUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PrismAlabaster
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
import com.example.ui.theme.PrismVioletContainer

@Composable
fun BrowserTopBar(
    uiState: BrowserUiState,
    onAddressChanged: (String) -> Unit,
    onAddressSubmit: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReloadOrStopClick: () -> Unit,
    onHomeClick: () -> Unit,
    onTogglePrivateMode: () -> Unit,
    onOpenTabsClick: () -> Unit,
    onOpenDiagnosticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val modeAccent by animateColorAsState(
        targetValue = if (uiState.isActiveTabPrivate) PrismViolet else PrismCyan,
        animationSpec = tween(220),
        label = "mode_accent"
    )
    val omniboxBorderColor by animateColorAsState(
        targetValue = when {
            isFocused -> modeAccent
            uiState.isActiveTabPrivate -> PrismViolet.copy(alpha = 0.5f)
            else -> PrismOutline
        },
        animationSpec = tween(200),
        label = "omnibox_border"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PrismObsidian)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Primary Omnibox Row: Shield Diagnostics + URL/Search Pill + Tab Switcher Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tappable Layer 1 & 2 Protection Status Pill
            val shieldOk = uiState.flagSecureCleared && uiState.documentStartScriptSupported
            Surface(
                onClick = onOpenDiagnosticsClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("protection_status_button"),
                shape = RoundedCornerShape(14.dp),
                color = if (shieldOk) PrismEmeraldContainer else PrismSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (shieldOk) PrismEmerald.copy(alpha = 0.55f) else PrismRose
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = stringResource(R.string.cd_protection_status),
                        tint = if (shieldOk) PrismEmerald else PrismRose,
                        modifier = Modifier.size(20.dp)
                    )
                    // Subtle live pulse dot in corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (shieldOk) PrismEmerald else PrismRose)
                    )
                }
            }

            // Matte-dark URL / Search Pill Input
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrismSurface)
                    .border(1.dp, omniboxBorderColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (uiState.isActiveTabPrivate) {
                        Icons.Rounded.VisibilityOff
                    } else {
                        Icons.Rounded.Public
                    },
                    contentDescription = null,
                    tint = modeAccent,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (uiState.addressInput.isEmpty()) {
                        Text(
                            text = if (uiState.isActiveTabPrivate) {
                                "Private search or URL"
                            } else {
                                stringResource(R.string.hint_address_bar)
                            },
                            color = PrismSlateMuted,
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = uiState.addressInput,
                        onValueChange = onAddressChanged,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = PrismAlabaster,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(modeAccent),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                onAddressSubmit()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input_field")
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            }
                    )
                }

                if (uiState.addressInput.isNotEmpty() && isFocused) {
                    IconButton(
                        onClick = { onAddressChanged("") },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clear_url_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_clear_url),
                            tint = PrismPewter,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tab Count Switcher Button (48dp touch target)
            Surface(
                onClick = onOpenTabsClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("open_tabs_button"),
                shape = RoundedCornerShape(14.dp),
                color = PrismSurface,
                border = BorderStroke(1.dp, PrismOutline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                width = 1.6.dp,
                                color = modeAccent,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.tabs.size.coerceAtLeast(1).toString(),
                            color = PrismAlabaster,
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // Secondary Precision Action Strip: Back / Forward / Reload / Home + Normal/Private Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                enabled = uiState.canGoBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = if (uiState.canGoBack) PrismAlabaster else PrismSlateMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onForwardClick,
                enabled = uiState.canGoForward,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_forward_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = stringResource(R.string.cd_forward),
                    tint = if (uiState.canGoForward) PrismAlabaster else PrismSlateMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onReloadOrStopClick,
                enabled = !uiState.showStartPage,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_reload_button")
            ) {
                Icon(
                    imageVector = if (uiState.isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                    contentDescription = stringResource(
                        if (uiState.isLoading) R.string.cd_stop else R.string.cd_reload
                    ),
                    tint = if (!uiState.showStartPage) PrismAlabaster else PrismSlateMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_home_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = "Start Page",
                    tint = if (uiState.showStartPage) modeAccent else PrismPewter,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Normal / Private Mode Toggle Chip
            val chipBg = if (uiState.isActiveTabPrivate) PrismVioletContainer else PrismCyanContainer
            val chipFg = if (uiState.isActiveTabPrivate) PrismViolet else PrismCyan
            Row(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(chipBg)
                    .border(1.dp, chipFg.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onTogglePrivateMode)
                    .padding(horizontal = 12.dp)
                    .testTag("mode_toggle_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isActiveTabPrivate) {
                        Icons.Rounded.VisibilityOff
                    } else {
                        Icons.Rounded.Public
                    },
                    contentDescription = stringResource(R.string.cd_toggle_private),
                    tint = chipFg,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (uiState.isActiveTabPrivate) {
                        stringResource(R.string.label_private)
                    } else {
                        stringResource(R.string.label_normal)
                    },
                    color = chipFg,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Thin animated progress bar or matte hairline divider
        AnimatedVisibility(visible = uiState.isLoading && uiState.loadProgress in 1..99) {
            val animatedProgress by animateFloatAsState(
                targetValue = (uiState.loadProgress / 100f).coerceIn(0.05f, 1f),
                animationSpec = tween(140),
                label = "load_progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = modeAccent,
                trackColor = PrismSurface
            )
        }

        if (!uiState.isLoading || uiState.loadProgress !in 1..99) {
            HorizontalDivider(
                thickness = 1.dp,
                color = PrismOutline.copy(alpha = 0.65f)
            )
        }
    }
}
