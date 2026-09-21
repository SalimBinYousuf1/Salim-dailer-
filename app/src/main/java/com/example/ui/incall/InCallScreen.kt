package com.example.ui.incall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioRoute
import com.example.data.model.CallStateInfo
import com.example.data.model.SalimCallState
import com.example.telecom.CallManager
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SquircleShape
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleRed
import com.example.ui.theme.OledBlack
import kotlin.math.roundToInt

@Composable
fun InCallScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callState by CallManager.callState.collectAsState()
    var showDtmfKeypad by remember { mutableStateOf(false) }
    var showQuickDeclineDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1420),
                        OledBlack,
                        OledBlack
                    )
                )
            )
            .testTag("in_call_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Caller Information Header
            val title = if (callState.displayName.isNotBlank()) callState.displayName else callState.number
            Text(
                text = if (title.isNotBlank()) title else "Unknown Caller",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("incall_caller_name")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle / Duration / State
            val statusText = when (callState.state) {
                SalimCallState.RINGING -> "Incoming Call..."
                SalimCallState.DIALING -> "Dialing..."
                SalimCallState.CONNECTING -> "Connecting..."
                SalimCallState.HOLDING -> "Call on Hold"
                SalimCallState.ACTIVE -> {
                    val sec = callState.durationSeconds
                    String.format("%02d:%02d", sec / 60, sec % 60)
                }
                SalimCallState.DISCONNECTED -> callState.disconnectReason?.ifEmpty { "Call Ended" } ?: "Call Ended"
                SalimCallState.IDLE -> "Call Ended"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = if (callState.state == SalimCallState.HOLDING) Color(0xFFFFD60A) else Color(0xFF8E8E93)
                ),
                modifier = Modifier.testTag("incall_status_text")
            )

            if (callState.displayName.isNotBlank() && callState.number.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = callState.number,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF636366)
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Central Area: either In-Call Controls or Incoming Slider
            if (callState.state == SalimCallState.RINGING) {
                IncomingCallControls(
                    onAnswer = { CallManager.answer() },
                    onDecline = { CallManager.reject() },
                    onMessage = { showQuickDeclineDialog = true }
                )
            } else {
                if (showDtmfKeypad) {
                    InCallDtmfGrid(
                        onDigit = { CallManager.playDtmf(it) },
                        onHide = { showDtmfKeypad = false }
                    )
                } else {
                    ActiveCallControlsCluster(
                        callState = callState,
                        onToggleMute = { CallManager.toggleMute() },
                        onToggleSpeaker = { CallManager.toggleSpeaker() },
                        onToggleHold = { CallManager.toggleHold() },
                        onShowKeypad = { showDtmfKeypad = true }
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // End Call Button
                EndCallButton(
                    onClick = {
                        CallManager.disconnect()
                        onClose()
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Quick Decline SMS Dialog
    if (showQuickDeclineDialog) {
        val defaultReplies = listOf(
            "Can't talk right now. What's up?",
            "I'll call you right back.",
            "I'm in a meeting. Will call later.",
            "I'm on my way."
        )

        AlertDialog(
            onDismissRequest = { showQuickDeclineDialog = false },
            title = { Text("Respond with Text", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    defaultReplies.forEach { msg ->
                        TextButton(
                            onClick = {
                                CallManager.reject(rejectWithMessage = true, textMessage = msg)
                                showQuickDeclineDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AppleBlue,
                                    textAlign = TextAlign.Start
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQuickDeclineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActiveCallControlsCluster(
    callState: CallStateInfo,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onShowKeypad: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = SquircleShape(28.dp),
        opacity = 0.85f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 1: Mute, Keypad, Audio Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InCallControlButton(
                    icon = if (callState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    label = "Mute",
                    isActive = callState.isMuted,
                    onClick = onToggleMute,
                    testTag = "incall_mute_button"
                )

                InCallControlButton(
                    icon = Icons.Rounded.Dialpad,
                    label = "Keypad",
                    isActive = false,
                    onClick = onShowKeypad,
                    testTag = "incall_keypad_button"
                )

                InCallControlButton(
                    icon = if (callState.audioRoute == AudioRoute.SPEAKER) Icons.Rounded.VolumeUp else Icons.Rounded.Headset,
                    label = if (callState.audioRoute == AudioRoute.SPEAKER) "Speaker" else "Audio",
                    isActive = callState.audioRoute == AudioRoute.SPEAKER,
                    onClick = onToggleSpeaker,
                    testTag = "incall_speaker_button"
                )
            }

            // Row 2: Hold, Add Call, Message
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InCallControlButton(
                    icon = if (callState.isHold) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    label = if (callState.isHold) "Unhold" else "Hold",
                    isActive = callState.isHold,
                    onClick = onToggleHold,
                    testTag = "incall_hold_button"
                )

                InCallControlButton(
                    icon = Icons.Rounded.PersonAdd,
                    label = "Add Call",
                    isActive = false,
                    onClick = {},
                    testTag = "incall_add_call_button"
                )

                InCallControlButton(
                    icon = Icons.Rounded.Message,
                    label = "Message",
                    isActive = false,
                    onClick = {},
                    testTag = "incall_message_button"
                )
            }
        }
    }
}

@Composable
private fun InCallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val activeColor = if (isActive) Color.White else null
    val iconColor = if (isActive) Color.Black else Color.White
    val labelColor = if (isActive) Color.White else Color(0xFF8E8E93)

    LiquidGlassButton(
        onClick = onClick,
        modifier = Modifier
            .size(width = 82.dp, height = 74.dp)
            .testTag(testTag),
        shape = SquircleShape(20.dp),
        activeColor = activeColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = labelColor
                )
            )
        }
    }
}

@Composable
private fun IncomingCallControls(
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onMessage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick decline text reply button
        IconButton(
            onClick = onMessage,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Message,
                contentDescription = "Message Reply",
                tint = AppleBlue,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Remind Me / Message",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF8E8E93)
            )
        )

        Spacer(modifier = Modifier.height(44.dp))

        // Apple-style Answer (Green) and Decline (Red) action buttons with spring scale
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(AppleRed)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                if (delta < -30f) onDecline()
                            }
                        )
                        .testTag("incoming_decline_button"),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onDecline, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Decline Call",
                            tint = Color.White,
                            modifier = Modifier
                                .size(34.dp)
                                .rotate(135f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Decline",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Answer Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(AppleGreen)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                if (delta > 30f) onAnswer()
                            }
                        )
                        .testTag("incoming_answer_button"),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onAnswer, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Answer Call",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Answer",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun EndCallButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(AppleRed)
            .testTag("incall_end_call_button"),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .rotate(135f)
            )
        }
    }
}

@Composable
private fun InCallDtmfGrid(
    onDigit: (Char) -> Unit,
    onHide: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = SquircleShape(24.dp),
        opacity = 0.90f
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rows = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('*', '0', '#')
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { digit ->
                        LiquidGlassButton(
                            onClick = { onDigit(digit) },
                            modifier = Modifier.size(60.dp),
                            shape = SquircleShape(16.dp)
                        ) {
                            Text(
                                text = digit.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onHide) {
                Text("Hide Keypad", color = AppleBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}
