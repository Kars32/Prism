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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.TabMetadata
import com.example.ui.theme.PrismAlabaster
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismCyanContainer
import com.example.ui.theme.PrismObsidian
import com.example.ui.theme.PrismOutline
import com.example.ui.theme.PrismPewter
import com.example.ui.theme.PrismRose
import com.example.ui.theme.PrismSlateMuted
import com.example.ui.theme.PrismSurface
import com.example.ui.theme.PrismSurfaceElevated
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.PrismVioletContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherSheet(
    tabs: List<TabMetadata>,
    onDismiss: () -> Unit,
    onSelectTab: (Long) -> Unit,
    onCloseTab: (Long) -> Unit,
    onNewNormalTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onWipeAllPrivateTabs: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val normalTabs = tabs.filter { !it.isPrivate }
    val privateTabs = tabs.filter { it.isPrivate }

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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Sheet Title & Creation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tabs",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PrismAlabaster
                    )
                    Text(
                        text = "Both Normal and Private sessions stay capture-ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrismPewter
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNewNormalTab,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("new_normal_tab_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrismCyanContainer,
                        contentColor = PrismCyan
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.cd_new_normal_tab),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Normal", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onNewPrivateTab,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("new_private_tab_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrismVioletContainer,
                        contentColor = PrismViolet
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = stringResource(R.string.cd_new_private_tab),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Private", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Normal Tabs Section
                item(key = "header_normal") {
                    SectionLabel(
                        title = "Normal Tabs (${normalTabs.size})",
                        accent = PrismCyan
                    )
                }

                if (normalTabs.isEmpty()) {
                    item(key = "empty_normal") {
                        Text(
                            text = "No normal tabs open.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrismSlateMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    items(normalTabs, key = { "normal_${it.id}" }) { tab ->
                        TabRowCard(
                            tab = tab,
                            onSelect = { onSelectTab(tab.id) },
                            onClose = { onCloseTab(tab.id) }
                        )
                    }
                }

                // Private Tabs Section
                item(key = "header_private") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel(
                            title = "Private Tabs (${privateTabs.size})",
                            accent = PrismViolet
                        )
                        if (privateTabs.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onWipeAllPrivateTabs,
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("wipe_all_private_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, PrismRose.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrismRose)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wipe All", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                if (privateTabs.isEmpty()) {
                    item(key = "empty_private") {
                        Text(
                            text = "No active private tabs. Private storage clears automatically when closed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrismSlateMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    items(privateTabs, key = { "private_${it.id}" }) { tab ->
                        TabRowCard(
                            tab = tab,
                            onSelect = { onSelectTab(tab.id) },
                            onClose = { onCloseTab(tab.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, accent: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = PrismAlabaster
        )
    }
}

@Composable
private fun TabRowCard(
    tab: TabMetadata,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val accent = if (tab.isPrivate) PrismViolet else PrismCyan
    val borderColor = if (tab.isActive) accent else PrismOutline

    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tab_item_${tab.id}"),
        shape = RoundedCornerShape(14.dp),
        color = if (tab.isActive) PrismSurfaceElevated else PrismSurface,
        border = BorderStroke(if (tab.isActive) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (tab.isPrivate) Icons.Rounded.VisibilityOff else Icons.Rounded.Public,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tab.title.ifBlank { if (tab.isPrivate) "Private Tab" else "Start Page" },
                    style = MaterialTheme.typography.titleSmall,
                    color = PrismAlabaster,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tab.url.ifBlank { "prism://start" },
                    style = MaterialTheme.typography.labelSmall,
                    color = PrismSlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("close_tab_${tab.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close_tab),
                    tint = PrismPewter,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
