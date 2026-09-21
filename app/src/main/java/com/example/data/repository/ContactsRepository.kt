package com.example.data.repository

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.example.data.model.ContactItem
import com.example.data.model.PhoneNumberInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun getContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<Long, ContactItem>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val lookupIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val starIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (it.moveToNext()) {
                    val contactId = it.getLong(idIdx)
                    val lookupKey = if (lookupIdx != -1) it.getString(lookupIdx) ?: "" else ""
                    val displayName = if (nameIdx != -1) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx != -1) it.getString(numberIdx) ?: "" else ""
                    val type = if (typeIdx != -1) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val photoUriStr = if (photoIdx != -1) it.getString(photoIdx) else null
                    val isStarred = if (starIdx != -1) it.getInt(starIdx) == 1 else false

                    val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources,
                        type,
                        ""
                    ).toString()

                    val phoneInfo = PhoneNumberInfo(
                        number = number,
                        typeLabel = if (typeLabel.isBlank()) "Mobile" else typeLabel
                    )

                    val existing = contactsMap[contactId]
                    if (existing != null) {
                        val updatedPhones = existing.phoneNumbers.toMutableList().apply {
                            if (none { p -> p.number == number }) add(phoneInfo)
                        }
                        contactsMap[contactId] = existing.copy(phoneNumbers = updatedPhones)
                    } else {
                        contactsMap[contactId] = ContactItem(
                            id = contactId,
                            lookupKey = lookupKey,
                            displayName = displayName,
                            phoneNumbers = listOf(phoneInfo),
                            photoUri = photoUriStr?.let { Uri.parse(it) },
                            isStarred = isStarred
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        contactsMap.values.sortedBy { it.displayName.lowercase() }
    }

    suspend fun getContactDetails(contactId: Long): ContactItem? = withContext(Dispatchers.IO) {
        val contacts = getContacts()
        val match = contacts.find { it.id == contactId } ?: return@withContext null

        // Fetch email if any
        var email: String? = null
        try {
            val emailCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )
            emailCursor?.use {
                if (it.moveToNext()) {
                    val idx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    if (idx != -1) email = it.getString(idx)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        match.copy(email = email)
    }

    suspend fun toggleStar(contactId: Long, isStarred: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (isStarred) 1 else 0)
            }
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            val updated = context.contentResolver.update(uri, values, null, null)
            updated > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteContact(contactId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            val deleted = context.contentResolver.delete(uri, null, null)
            deleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addContact(name: String, phoneNumber: String, email: String?): Boolean = withContext(Dispatchers.IO) {
        val ops = ArrayList<ContentProviderOperation>()

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        if (!email.isNullOrBlank()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                    .build()
            )
        }

        try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
