package com.example.data.model

import android.net.Uri

data class PhoneNumberInfo(
    val number: String,
    val typeLabel: String,
    val isPrimary: Boolean = false
)

data class ContactItem(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val phoneNumbers: List<PhoneNumberInfo> = emptyList(),
    val email: String? = null,
    val photoUri: Uri? = null,
    val isStarred: Boolean = false
)
