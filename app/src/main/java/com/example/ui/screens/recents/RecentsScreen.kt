@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui.screens.recents

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallLogEntry
import com.example.data.model.CallType
import com.example.ui.components.DefaultDialerBanner
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SquircleShape
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleRed
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    viewModel: RecentsViewModel,
    onRequestDefaultDialer: () -> Unit,
    onNavigateToAddContact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedEntryForSheet by remember { mutableStateOf<CallLogEntry?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCallLogs()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("recents_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recents",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            // Segmented Control (All | Missed) in Liquid Glass
            LiquidGlassCard(
                shape = SquircleShape(12.dp),
                opacity = 0.85f,
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SegmentButton(
                        text = "All",
                        selected = !uiState.filterMissed,
                        onClick = { viewModel.setFilter(false) }
                    )
                    SegmentButton(
                        text = "Missed",
                        selected = uiState.filterMissed,
                        onClick = { viewModel.setFilter(true) }
                    )
                }
            }

            if (uiState.callLogs.isNotEmpty()) {
                TextButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("recents_clear_all_button")
                ) {
                    Text(
                        text = "Clear",
                        color = AppleRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        // Default dialer reminder if needed
        DefaultDialerBanner(
            isDefaultDialer = uiState.isDefaultDialer,
            onRequestDefault = onRequestDefaultDialer
        )

        // Content: Skeletons or Real Call Log List
        if (uiState.isLoading) {
            RecentsSkeletonList()
        } else if (uiState.callLogs.isEmpty()) {
            EmptyRecentsView(filterMissed = uiState.filterMissed)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("recents_call_list")
            ) {
                items(
                    items = uiState.callLogs,
                    key = { it.id }
                ) { entry ->
                    SwipeableCallLogRow(
                        entry = entry,
                        onClick = { viewModel.callBack(entry.number) },
                        onLongClick = { selectedEntryForSheet = entry },
                        onInfoClick = { selectedEntryForSheet = entry },
                        onDelete = { viewModel.deleteCall(entry) }
                    )
                }
            }
        }
    }

    // Action Sheet for long-pressed call log entry
    selectedEntryForSheet?.let { entry ->
        CallLogActionSheet(
            entry = entry,
            onDismiss = { selectedEntryForSheet = null },
            onCall = {
                selectedEntryForSheet = null
                viewModel.callBack(entry.number)
            },
            onMessage = {
                selectedEntryForSheet = null
                viewModel.sendSms(entry.number)
            },
            onCopy = {
                selectedEntryForSheet = null
                viewModel.copyNumber(entry.number)
            },
            onAddContact = {
                selectedEntryForSheet = null
                onNavigateToAddContact(entry.number)
            },
            onBlock = {
                selectedEntryForSheet = null
                viewModel.blockNumber(entry.number)
            },
            onDelete = {
                selectedEntryForSheet = null
                viewModel.deleteCall(entry)
            }
        )
    }

    // Confirm Clear All Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Recents", fontWeight = FontWeight.SemiBold) },
            text = { Text("Are you sure you want to delete your entire call history from this device?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllLogs()
                    }
                ) {
                    Text("Clear All", color = AppleRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(SquircleShape(10.dp))
            .background(bg)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCallLogRow(
    entry: CallLogEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = AppleRed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        CallLogRowContent(
            entry = entry,
            onClick = onClick,
            onLongClick = onLongClick,
            onInfoClick = onInfoClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallLogRowContent(
    entry: CallLogEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val isMissed = entry.type == CallType.MISSED || entry.type == CallType.REJECTED
    val titleColor = if (isMissed) AppleRed else MaterialTheme.colorScheme.onSurface

    val relativeTime = DateUtils.getRelativeTimeSpanString(
        entry.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()

    val (icon, iconTint) = when (entry.type) {
        CallType.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived to MaterialTheme.colorScheme.onSurfaceVariant
        CallType.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade to MaterialTheme.colorScheme.onSurfaceVariant
        CallType.MISSED, CallType.REJECTED -> Icons.AutoMirrored.Rounded.CallMissed to AppleRed
        CallType.BLOCKED -> Icons.Rounded.Block to AppleRed
        CallType.VOICEMAIL -> Icons.Rounded.Call to AppleBlue
        else -> Icons.AutoMirrored.Rounded.CallReceived to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val displayName = if (!entry.name.isNullOrBlank()) entry.name else entry.formattedNumber

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("call_log_row_${entry.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Direction Icon
        Icon(
            imageVector = icon,
            contentDescription = entry.type.name,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Name / Number and Relative Time
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    ),
                    maxLines = 1
                )
                if (entry.count > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${entry.count})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (!entry.name.isNullOrBlank()) "${entry.formattedNumber} • $relativeTime" else relativeTime,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Info Button
        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Call Details",
                tint = AppleBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallLogActionSheet(
    entry: CallLogEntry,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onCopy: () -> Unit,
    onAddContact: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = SquircleShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Caller summary
            Text(
                text = if (!entry.name.isNullOrBlank()) entry.name else entry.formattedNumber,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = entry.formattedNumber,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Items
            SheetActionItem(
                icon = Icons.Rounded.Call,
                label = "Call ${entry.formattedNumber}",
                tint = AppleGreen,
                onClick = onCall
            )
            SheetActionItem(
                icon = Icons.Rounded.Message,
                label = "Send Message",
                tint = AppleBlue,
                onClick = onMessage
            )
            SheetActionItem(
                icon = Icons.Rounded.ContentCopy,
                label = "Copy Number",
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onCopy
            )
            if (entry.name.isNullOrBlank()) {
                SheetActionItem(
                    icon = Icons.Rounded.PersonAdd,
                    label = "Create New Contact",
                    tint = AppleBlue,
                    onClick = onAddContact
                )
            }
            SheetActionItem(
                icon = Icons.Rounded.Block,
                label = "Block this Number",
                tint = AppleRed,
                onClick = onBlock
            )
            SheetActionItem(
                icon = Icons.Rounded.Delete,
                label = "Delete from Recents",
                tint = AppleRed,
                onClick = onDelete
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SheetActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape(14.dp))
            .combinedClickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = tint
            )
        )
    }
}

@Composable
private fun EmptyRecentsView(filterMissed: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (filterMissed) Icons.AutoMirrored.Rounded.CallMissed else Icons.Rounded.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (filterMissed) "No Missed Calls" else "No Recent Calls",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (filterMissed) "You have no missed calls." else "Your incoming and outgoing calls will appear here.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun RecentsSkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(8) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(18.dp)
                            .clip(SquircleShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                            .clip(SquircleShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    }
}
