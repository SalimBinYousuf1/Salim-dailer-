package com.example.ui.screens.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import com.example.data.model.ContactItem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SquircleShape
import com.example.ui.screens.contacts.ContactsViewModel
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    contactsViewModel: ContactsViewModel,
    onNavigateToContactDetail: (ContactItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val contactsState by contactsViewModel.uiState.collectAsState()
    val favoriteContacts = contactsState.contacts.filter { it.isStarred }

    var isEditMode by remember { mutableStateOf(false) }
    var showAddPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_screen")
    ) {
        // iOS Style Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Edit button (top left like Apple iOS)
            TextButton(
                onClick = { isEditMode = !isEditMode },
                enabled = favoriteContacts.isNotEmpty()
            ) {
                Text(
                    text = if (isEditMode) "Done" else "Edit",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (favoriteContacts.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else AppleBlue,
                        fontWeight = if (isEditMode) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }

            Text(
                text = "Favorites",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            // Plus button to add a contact to favorites
            IconButton(
                onClick = { showAddPicker = true },
                modifier = Modifier.testTag("favorites_add_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add to Favorites",
                    tint = AppleBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (favoriteContacts.isEmpty()) {
            // Apple Minimalist Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "No Favorites",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Quickly call, message, or FaceTime the contacts you reach out to most.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showAddPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                        shape = SquircleShape(16.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Favorites", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            // Favorites List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favoriteContacts, key = { it.id }) { contact ->
                    FavoriteContactCard(
                        contact = contact,
                        isEditMode = isEditMode,
                        onCall = {
                            val number = contact.phoneNumbers.firstOrNull()?.number
                            if (!number.isNullOrBlank()) {
                                contactsViewModel.callContact(number)
                            }
                        },
                        onMessage = {
                            val number = contact.phoneNumbers.firstOrNull()?.number
                            if (!number.isNullOrBlank()) {
                                contactsViewModel.messageContact(number)
                            }
                        },
                        onRemove = {
                            contactsViewModel.toggleStar(contact)
                        },
                        onClick = {
                            if (!isEditMode) {
                                val number = contact.phoneNumbers.firstOrNull()?.number
                                if (!number.isNullOrBlank()) {
                                    contactsViewModel.callContact(number)
                                } else {
                                    onNavigateToContactDetail(contact)
                                }
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Add to Favorites Contact Picker Sheet
    if (showAddPicker) {
        AddFavoritePickerDialog(
            contacts = contactsState.contacts,
            onDismiss = { showAddPicker = false },
            onSelectContact = { contact ->
                if (!contact.isStarred) {
                    contactsViewModel.toggleStar(contact)
                }
                showAddPicker = false
            }
        )
    }
}

@Composable
private fun FavoriteContactCard(
    contact: ContactItem,
    isEditMode: Boolean,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val phoneInfo = contact.phoneNumbers.firstOrNull()
    val phoneLabel = phoneInfo?.typeLabel ?: "mobile"
    val phoneNumber = phoneInfo?.number ?: ""

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("favorite_card_${contact.id}"),
        shape = SquircleShape(18.dp),
        opacity = 0.90f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Edit delete circle icon if in edit mode
                AnimatedVisibility(visible = isEditMode) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(AppleRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Apple Monogram Avatar
                val initials = contact.displayName.split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .map { it.first().uppercaseChar() }
                    .joinToString("")
                    .ifEmpty { "?" }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$phoneLabel  $phoneNumber",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Quick Call and Message Action Pills
            if (!isEditMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick SMS button
                    IconButton(
                        onClick = onMessage,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Message,
                            contentDescription = "Message",
                            tint = AppleBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Direct Call Button
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AppleGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Call",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFavoritePickerDialog(
    contacts: List<ContactItem>,
    onDismiss: () -> Unit,
    onSelectContact: (ContactItem) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, contacts) {
        if (search.isBlank()) contacts else {
            val q = search.trim().lowercase()
            contacts.filter { it.displayName.lowercase().contains(q) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Choose a Contact", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = SquircleShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectContact(c) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val initials = c.displayName.take(1).uppercase()
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initials, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = c.displayName,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                c.phoneNumbers.firstOrNull()?.let {
                                    Text(
                                        text = it.number,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
