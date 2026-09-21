package com.example.ui.screens.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.QuickDeclineMessageEntity
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SquircleShape
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleRed

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onRequestDefaultDialer: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<QuickDeclineMessageEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.checkDefaultDialer()
        viewModel.loadBlockedNumbers()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .testTag("settings_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            if (onClose != null) {
                TextButton(onClick = onClose) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = AppleBlue,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Group 1: Default Phone App
        SettingsSectionHeader(title = "TELEPHONY ROLE")
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = SquircleShape(18.dp),
            opacity = 0.85f
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = null,
                            tint = if (uiState.isDefaultDialer) AppleGreen else AppleBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Default Phone App",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (uiState.isDefaultDialer) "Salim is active default dialer" else "System dialer not assigned",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (uiState.isDefaultDialer) AppleGreen else AppleRed
                                )
                            )
                        }
                    }

                    if (!uiState.isDefaultDialer) {
                        Button(
                            onClick = onRequestDefaultDialer,
                            colors = ButtonDefaults.buttonColors(containerColor = AppleGreen),
                            shape = SquircleShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Set Default", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Active",
                            tint = AppleGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Group 2: Appearance & Material
        SettingsSectionHeader(title = "APPEARANCE")
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = SquircleShape(18.dp),
            opacity = 0.85f
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    icon = Icons.Rounded.ColorLens,
                    iconTint = AppleBlue,
                    title = "Dark Mode",
                    subtitle = if (uiState.isDarkMode) "OLED True Black active" else "Apple Premium White background active",
                    checked = uiState.isDarkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )

                if (uiState.isDarkMode) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    SettingsSwitchRow(
                        icon = Icons.Rounded.ColorLens,
                        iconTint = AppleBlue,
                        title = "True Black OLED",
                        subtitle = "Pure #000000 black canvas for AMOLED efficiency",
                        checked = uiState.isTrueBlack,
                        onCheckedChange = { viewModel.setTrueBlack(it) }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Liquid Glass Opacity",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "${(uiState.glassOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AppleBlue
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Controls physical thickness and specular highlight depth",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Slider(
                        value = uiState.glassOpacity,
                        onValueChange = { viewModel.setGlassOpacity(it) },
                        valueRange = 0.35f..0.98f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppleBlue,
                            activeTrackColor = AppleBlue
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Group 3: Audio & Haptics
        SettingsSectionHeader(title = "SOUNDS & HAPTICS")
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = SquircleShape(18.dp),
            opacity = 0.85f
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    icon = Icons.Rounded.VolumeUp,
                    iconTint = AppleGreen,
                    title = "Keypad Tones (DTMF)",
                    subtitle = "Play real-time acoustic tones on touch down",
                    checked = uiState.dialpadSound,
                    onCheckedChange = { viewModel.setDialpadSound(it) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                SettingsSwitchRow(
                    icon = Icons.Rounded.Phone,
                    iconTint = AppleBlue,
                    title = "Keypad Haptics",
                    subtitle = "Physical click tick on touch contact",
                    checked = uiState.dialpadHaptic,
                    onCheckedChange = { viewModel.setDialpadHaptic(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Group 4: Call Screening & Spam
        SettingsSectionHeader(title = "CALL SCREENING & SPAM BLOCKING")
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = SquircleShape(18.dp),
            opacity = 0.85f
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                uiState.screeningRules.forEachIndexed { index, rule ->
                    val (title, subtitle) = when (rule.id) {
                        "block_private" -> "Block Private & Restricted Numbers" to "Automatically reject anonymous callers without ringing"
                        "block_unknown" -> "Block Unknown Callers" to "Reject callers not saved in your Contacts list"
                        else -> rule.id to "Filter unwanted calls"
                    }

                    SettingsSwitchRow(
                        icon = Icons.Rounded.Security,
                        iconTint = AppleRed,
                        title = title,
                        subtitle = subtitle,
                        checked = rule.enabled,
                        onCheckedChange = { isChecked ->
                            viewModel.toggleScreeningRule(rule.id, isChecked)
                        }
                    )
                    if (index < uiState.screeningRules.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Group 5: Blocked Numbers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsSectionHeader(title = "BLOCKED NUMBERS")
            IconButton(onClick = { showAddBlockDialog = true }) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Block Number",
                    tint = AppleBlue
                )
            }
        }

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = SquircleShape(18.dp),
            opacity = 0.85f
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (uiState.blockedNumbers.isEmpty()) {
                    Text(
                        text = "No blocked numbers. Blocked numbers will be automatically rejected without ringing.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                } else {
                    uiState.blockedNumbers.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.number.ifBlank { item.originalNumber },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            IconButton(
                                onClick = { viewModel.unblockNumber(item.originalNumber) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Unblock",
                                    tint = AppleRed
                                )
                            }
                        }
                        if (index < uiState.blockedNumbers.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Group 6: Quick Decline Messages
        SettingsSectionHeader(title = "RESPOND WITH TEXT (QUICK DECLINE)")
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = SquircleShape(18.dp),
            opacity = 0.85f
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                uiState.quickDeclineMessages.forEachIndexed { index, msg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingMessage = msg }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\"${msg.text}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            tint = AppleBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (index < uiState.quickDeclineMessages.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    // Dialog: Add Blocked Number
    if (showAddBlockDialog) {
        var numToBlock by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Block a Phone Number", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = numToBlock,
                    onValueChange = { numToBlock = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (numToBlock.isNotBlank()) {
                            viewModel.addBlockedNumber(numToBlock.trim())
                        }
                        showAddBlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleRed)
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Edit Quick Decline Message
    editingMessage?.let { msg ->
        var updatedText by remember(msg.id) { mutableStateOf(msg.text) }
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Quick Decline Reply", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = updatedText,
                    onValueChange = { updatedText = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (updatedText.isNotBlank()) {
                            viewModel.updateQuickDeclineMessage(msg.id, updatedText.trim())
                        }
                        editingMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.1.sp
        ),
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppleGreen
            )
        )
    }
}
