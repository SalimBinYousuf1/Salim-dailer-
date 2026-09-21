@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui.screens.dialpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SpeedDialEntry
import com.example.ui.components.DefaultDialerBanner
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SquircleShape
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.DarkKeypadKey
import com.example.ui.theme.DarkKeypadKeyPressed
import com.example.ui.theme.LightKeypadKey
import com.example.ui.theme.LightKeypadKeyPressed
import com.example.ui.theme.LocalIsTrueBlack
import com.example.ui.theme.LocalLiquidGlassOpacity

data class KeypadItem(val digit: Char, val subtext: String)

private val keypadRows = listOf(
    listOf(KeypadItem('1', ""), KeypadItem('2', "A B C"), KeypadItem('3', "D E F")),
    listOf(KeypadItem('4', "G H I"), KeypadItem('5', "J K L"), KeypadItem('6', "M N O")),
    listOf(KeypadItem('7', "P Q R S"), KeypadItem('8', "T U V"), KeypadItem('9', "W X Y Z")),
    listOf(KeypadItem('*', ""), KeypadItem('0', "+"), KeypadItem('#', ""))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialPadScreen(
    viewModel: DialPadViewModel,
    onRequestDefaultDialer: () -> Unit,
    onNavigateToAddContact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    var speedDialSlotToAssign by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshDialerStatus()
        viewModel.loadSimCards()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("dialpad_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Default dialer role prompt banner if needed
        DefaultDialerBanner(
            isDefaultDialer = uiState.isDefaultDialer,
            onRequestDefault = onRequestDefaultDialer
        )

        Spacer(modifier = Modifier.weight(0.7f))

        // Number Display Area (Single Focal Point)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (uiState.formattedNumber.isNotEmpty()) uiState.formattedNumber else "",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = if (uiState.formattedNumber.length > 12) 28.sp else 34.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("dialpad_number_display")
                )

                AnimatedVisibility(
                    visible = uiState.enteredNumber.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(
                        onClick = { onNavigateToAddContact(uiState.enteredNumber) },
                        modifier = Modifier.testTag("add_number_to_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = "Add to Contacts",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add to Contacts",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad Grid with Apple squircle buttons & spring physics
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in keypadRows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (item in row) {
                        KeypadKey(
                            item = item,
                            onClick = {
                                if (uiState.enableHaptic) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                viewModel.onDigitPress(item.digit)
                            },
                            onLongClick = {
                                if (uiState.enableHaptic) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                val result = viewModel.onDigitLongPress(item.digit)
                                if (item.digit in '2'..'9' && result == null) {
                                    // Empty speed dial slot, prompt user to assign
                                    speedDialSlotToAssign = item.digit.digitToInt()
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dual-SIM Selector Pill (if multi-SIM)
        if (uiState.simCards.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                uiState.simCards.forEachIndexed { index, sim ->
                    val isSelected = index == uiState.selectedSimIndex
                    Button(
                        onClick = { viewModel.selectSim(index) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) AppleGreen else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = SquircleShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "${sim.displayName} (${sim.carrierName})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Bottom Action Bar: Call Button (Centered) and Backspace (Right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            contentAlignment = Alignment.Center
        ) {
            // Large circular call button
            CallButton(
                onClick = {
                    if (uiState.enableHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.placeCall()
                },
                enabled = uiState.enteredNumber.isNotEmpty()
            )

            // Backspace button positioned to the right
            if (uiState.enteredNumber.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                if (uiState.enableHaptic) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                viewModel.onBackspace()
                            },
                            onLongClick = {
                                if (uiState.enableHaptic) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                viewModel.onClearAll()
                            }
                        )
                        .testTag("dialpad_backspace_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = "Delete Digit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.4f))
    }

    // Speed dial assignment dialog
    speedDialSlotToAssign?.let { slot ->
        var nameInput by remember { mutableStateOf("") }
        var numberInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { speedDialSlotToAssign = null },
            title = {
                Text(
                    text = "Assign Speed Dial Key $slot",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter contact details to assign to speed dial key $slot:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = { numberInput = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (numberInput.isNotBlank()) {
                            // Assign in background
                            viewModel.setNumber(numberInput)
                        }
                        speedDialSlotToAssign = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleGreen)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { speedDialSlotToAssign = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadKey(
    item: KeypadItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Apple spring response: instantaneous scale down on touch, bouncy settle
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "key_scale"
    )

    val isDark = MaterialTheme.colorScheme.surface.red < 0.2f
    val baseColor = if (isDark) {
        if (isPressed) DarkKeypadKeyPressed else DarkKeypadKey
    } else {
        if (isPressed) LightKeypadKeyPressed else LightKeypadKey
    }

    Box(
        modifier = modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(baseColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null, // Spring scale provides natural physical feedback
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("keypad_digit_${item.digit}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.digit.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (item.digit in listOf('*', '#')) 32.sp else 34.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                lineHeight = 36.sp
            )
            if (item.subtext.isNotEmpty()) {
                Text(
                    text = item.subtext,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun CallButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "call_btn_scale"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(AppleGreen)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("dialpad_call_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = "Place Call",
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
    }
}
