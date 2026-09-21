package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.SalimCallState
import com.example.telecom.CallManager
import com.example.telecom.TelecomHelper
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SquircleShape
import com.example.ui.incall.InCallActivity
import com.example.ui.navigation.SalimBottomBar
import com.example.ui.navigation.SalimTab
import com.example.ui.screens.contacts.AddEditContactScreen
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.contacts.ContactsViewModel
import com.example.ui.screens.dialpad.DialPadScreen
import com.example.ui.screens.dialpad.DialPadViewModel
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.recents.RecentsScreen
import com.example.ui.screens.recents.RecentsViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.voicemail.VoicemailScreen
import com.example.ui.screens.voicemail.VoicemailViewModel
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleRed
import com.example.ui.theme.MyApplicationTheme

@Composable
fun MainAppScreen(
    initialNumberFromIntent: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dialPadViewModel: DialPadViewModel = viewModel()
    val recentsViewModel: RecentsViewModel = viewModel()
    val contactsViewModel: ContactsViewModel = viewModel()
    val voicemailViewModel: VoicemailViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val settingsState by settingsViewModel.uiState.collectAsState()
    val activeCallState by CallManager.callState.collectAsState()

    var currentTab by remember { mutableStateOf(SalimTab.KEYPAD) }
    var addContactNumber by remember { mutableStateOf<String?>(null) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    // Role launcher for Default Phone App
    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        dialPadViewModel.refreshDialerStatus()
        recentsViewModel.loadCallLogs()
        settingsViewModel.checkDefaultDialer()
    }

    val requestDefaultDialer: () -> Unit = {
        val intent = TelecomHelper.createDefaultDialerIntent(context)
        if (intent != null) {
            defaultDialerLauncher.launch(intent)
        }
    }

    // Permission launcher for required telephony permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        dialPadViewModel.refreshDialerStatus()
        dialPadViewModel.loadSimCards()
        recentsViewModel.loadCallLogs()
        contactsViewModel.loadContacts()
    }

    LaunchedEffect(Unit) {
        val requiredPerms = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPerms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = requiredPerms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }

        initialNumberFromIntent?.let {
            if (it.isNotBlank()) {
                dialPadViewModel.setNumber(it)
                currentTab = SalimTab.KEYPAD
            }
        }
    }

    MyApplicationTheme(
        darkTheme = settingsState.isDarkMode,
        trueBlack = settingsState.isTrueBlack,
        glassOpacity = settingsState.glassOpacity
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            bottomBar = {
                SalimBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Ongoing Call Banner if call is active while browsing app
                    AnimatedVisibility(
                        visible = activeCallState.state == SalimCallState.ACTIVE || activeCallState.state == SalimCallState.HOLDING,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable {
                                    val intent = Intent(context, InCallActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    context.startActivity(intent)
                                }
                                .testTag("active_call_banner"),
                            shape = SquircleShape(16.dp),
                            opacity = 0.90f
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Call,
                                        contentDescription = null,
                                        tint = AppleGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Touch to return to call",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                                val sec = activeCallState.durationSeconds
                                Text(
                                    text = String.format("%02d:%02d", sec / 60, sec % 60),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppleGreen
                                    )
                                )
                            }
                        }
                    }

                    // Active Tab Screen
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            SalimTab.FAVORITES -> FavoritesScreen(
                                contactsViewModel = contactsViewModel,
                                onNavigateToContactDetail = { contact ->
                                    contactsViewModel.selectContact(contact)
                                    currentTab = SalimTab.CONTACTS
                                }
                            )
                            SalimTab.RECENTS -> RecentsScreen(
                                viewModel = recentsViewModel,
                                onRequestDefaultDialer = requestDefaultDialer,
                                onNavigateToAddContact = { addContactNumber = it }
                            )
                            SalimTab.CONTACTS -> ContactsScreen(
                                viewModel = contactsViewModel,
                                onNavigateToAddContact = { addContactNumber = "" }
                            )
                            SalimTab.KEYPAD -> DialPadScreen(
                                viewModel = dialPadViewModel,
                                onRequestDefaultDialer = requestDefaultDialer,
                                onNavigateToAddContact = { addContactNumber = it }
                            )
                            SalimTab.VOICEMAIL -> VoicemailScreen(
                                viewModel = voicemailViewModel
                            )
                        }
                    }
                }

                // Apple Settings Action Button (top right corner, sleek and accessible)
                IconButton(
                    onClick = { showSettingsScreen = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        .testTag("open_settings_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        if (!settingsState.isDefaultDialer) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppleRed)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }

                // Settings Screen Sheet / Full-Screen Overlay
                AnimatedVisibility(
                    visible = showSettingsScreen,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onRequestDefaultDialer = requestDefaultDialer,
                            onClose = { showSettingsScreen = false }
                        )
                    }
                }

                // Add Contact Sheet/Modal
                addContactNumber?.let { number ->
                    AddEditContactScreen(
                        initialNumber = number,
                        onDismiss = { addContactNumber = null },
                        onSave = { name, phone, email ->
                            contactsViewModel.addContact(name, phone, email) {
                                addContactNumber = null
                            }
                        }
                    )
                }
            }
        }
    }
}
